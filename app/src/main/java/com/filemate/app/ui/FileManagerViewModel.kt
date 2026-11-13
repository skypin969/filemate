package com.filemate.app.ui

import kotlinx.coroutines.delay
import com.filemate.app.data.Volumes
import androidx.core.content.ContextCompat
import android.hardware.usb.UsbManager
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.Intent
import android.content.Context
import android.content.BroadcastReceiver
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemate.app.R
import com.filemate.app.apk.ApkTools
import com.filemate.app.data.Archive
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import com.filemate.app.data.FileTools
import com.filemate.app.data.RootShell
import com.filemate.app.data.Trash
import com.filemate.app.mcp.McpService
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode { NAME, SIZE, DATE }

/** Filter jenis berkas yang tampil di panel. */
enum class TypeFilter { ALL, FOLDER, APK, MEDIA, DOCUMENT }

/**
 * Keadaan satu panel: folder yang dibuka, isinya, item yang dipilih,
 * dan riwayat folder untuk tombol kembali dan maju.
 */
data class PanelState(
    val path: String,
    val items: List<FileItem> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val selected: Set<String> = emptySet(),
    val back: List<String> = emptyList(),
    val forward: List<String> = emptyList(),
)

/** Berkas yang sudah disalin atau dipotong, menunggu ditempel. */
data class Clip(val paths: List<String>, val move: Boolean)

/** Tindakan yang mengubah berkas. */
sealed interface FileAction {
    /** [toTrash] true berarti berkas dipindah ke Recycle Bin, bukan dihapus selamanya. */
    data class Delete(val paths: List<String>, val toTrash: Boolean = false) : FileAction
    data class Paste(val sources: List<String>, val move: Boolean, val destDir: String) : FileAction
    data class Rename(val path: String, val newName: String) : FileAction
    data class NewFolder(val parentDir: String, val name: String) : FileAction
    data class NewFile(val parentDir: String, val name: String) : FileAction
}

/** Dialog yang sedang tampil. */
sealed interface AppDialog {
    data class NewFolder(val parentDir: String) : AppDialog
    data class Rename(val path: String) : AppDialog
    data class Confirm(val action: FileAction, val system: Boolean) : AppDialog
    data class Apk(val path: String) : AppDialog
    data class Attributes(val item: FileItem) : AppDialog
    /** Dialog "Ekstrak" untuk berkas arsip di [path]. */
    data class Extract(val path: String) : AppDialog
    data object TypeFilterPicker : AppDialog
}

/** Keadaan dialog alat APK. */
data class ApkInfo(
    val path: String,
    val manifest: String = "",
    val busy: Boolean = false,
    val status: String = "",
)

data class UiState(
    // Dua panel langsung aktif saat aplikasi dibuka, keduanya mulai dari penyimpanan utama
    val dual: Boolean = true,
    val rootMode: Boolean = false,
    val active: Int = 0,
    val panels: List<PanelState> = listOf(
        PanelState(FileRepository.STORAGE_ROOT),
        PanelState(FileRepository.STORAGE_ROOT),
    ),
    val sort: SortMode = SortMode.NAME,
    val search: String = "",
    val searchOpen: Boolean = false,
    val showHidden: Boolean = false,
    val typeFilter: TypeFilter = TypeFilter.ALL,
    val bookmarks: List<String> = emptyList(),
    val clip: Clip? = null,
    val dialog: AppDialog? = null,
    val apk: ApkInfo? = null,
    val message: String? = null,
    /** Berkas yang baru saja disalin atau dipindah. Namanya diberi warna di daftar supaya mudah ditemukan. */
    val fresh: Set<String> = emptySet(),
)

private val MEDIA_EXT = setOf(
    "JPG", "JPEG", "PNG", "GIF", "WEBP", "BMP", "HEIC",
    "MP4", "MKV", "3GP", "WEBM", "MP3", "M4A", "WAV", "OGG", "FLAC",
)

private val DOCUMENT_EXT = setOf(
    "PDF", "DOC", "DOCX", "TXT", "MD", "HTML", "HTM", "RTF", "ODT",
    "XLS", "XLSX", "PPT", "PPTX", "CSV",
)

/**
 * Berkas yang tampil di panel: sudah disaring pencarian, berkas tersembunyi,
 * dan jenis berkas. Urutan tetap seperti di [items].
 */
fun visibleItems(
    items: List<FileItem>,
    search: String,
    showHidden: Boolean,
    typeFilter: TypeFilter,
): List<FileItem> = items.filter { item ->
    (showHidden || !item.name.startsWith(".")) &&
        (search.isBlank() || item.name.contains(search, ignoreCase = true)) &&
        when (typeFilter) {
            TypeFilter.ALL -> true
            TypeFilter.FOLDER -> item.isDir
            TypeFilter.APK -> item.isApk
            TypeFilter.MEDIA -> !item.isDir && item.extension in MEDIA_EXT
            TypeFilter.DOCUMENT -> !item.isDir && item.extension in DOCUMENT_EXT
        }
}

class FileManagerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPrefs.of(app)

    private val _state = MutableStateFlow(
        UiState(
            bookmarks = loadBookmarks(),
            panels = listOf(
                PanelState(startFolder(AppPrefs.KEY_HOME_LEFT)),
                PanelState(startFolder(AppPrefs.KEY_HOME_RIGHT)),
            ),
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        reloadAll()
        // Akses root dicoba otomatis setiap aplikasi dibuka. Jika izin sudah diberikan di aplikasi
        // pengelola root (Magisk, KernelSU), mode root langsung aktif tanpa tombol.
        setRootMode(true, quiet = true)
        watchVolumes(app)
        // Pengaturan "Membersihkan Recycle Bin secara otomatis"
        val trashDays = prefs.getInt(AppPrefs.KEY_TRASH_DAYS, 0)
        if (trashDays > 0) {
            viewModelScope.launch(Dispatchers.IO) { Trash.purgeOlderThan(trashDays) }
        }
        // Sakelar "Mulai layanan secara otomatis saat aplikasi dibuka" di dialog APK MCP
        if (prefs.getBoolean(AppPrefs.KEY_MCP_AUTO, false)) {
            try {
                McpService.start(app)
            } catch (e: Exception) {
                // Android bisa menolak jika aplikasi belum tampil di layar. Layanan bisa dimulai manual.
            }
        }
    }

    // ---------- Flashdisk OTG dan kartu SD ----------

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshVolumes(announce = true)
            // Saat flashdisk baru dicolok, Android butuh waktu sebentar sebelum isinya bisa dibaca
            viewModelScope.launch {
                delay(2500)
                refreshVolumes(announce = true)
            }
        }
    }

    /** Memantau flashdisk yang dicolok atau dilepas selama aplikasi terbuka. */
    private fun watchVolumes(app: Application) {
        val media = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }
        val usb = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        try {
            ContextCompat.registerReceiver(app, volumeReceiver, media, ContextCompat.RECEIVER_EXPORTED)
            ContextCompat.registerReceiver(app, volumeReceiver, usb, ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            // Tanpa pemantau, daftar tetap diperbarui setiap menu samping dibuka
        }
        refreshVolumes()
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            // Belum terdaftar
        }
        super.onCleared()
    }

    /** Mencari ulang flashdisk yang terpasang. [announce] menampilkan pesan saat ada yang dicolok atau dilepas. */
    fun refreshVolumes(announce: Boolean = false) {
        viewModelScope.launch {
            val root = _state.value.rootMode
            val found = withContext(Dispatchers.IO) { Volumes.scan(getApplication<Application>(), root) }
            val before = Volumes.list.value
            if (found == before) return@launch
            Volumes.update(found)
            val added = found.filter { v -> before.none { it.path == v.path } }
            val removed = before.filter { v -> found.none { it.path == v.path } }
            // Panel yang sedang membuka flashdisk yang dilepas dikembalikan ke penyimpanan internal
            val gone = { path: String -> removed.any { path == it.path || path.startsWith(it.path + "/") } }
            if (_state.value.panels.any { gone(it.path) }) {
                _state.update { s ->
                    s.copy(
                        panels = s.panels.map { p ->
                            if (!gone(p.path)) p.copy(back = p.back.filterNot(gone), forward = p.forward.filterNot(gone))
                            else p.copy(
                                path = FileRepository.STORAGE_ROOT,
                                selected = emptySet(),
                                back = p.back.filterNot(gone),
                                forward = p.forward.filterNot(gone),
                            )
                        },
                    )
                }
                reloadAll()
            }
            if (announce) {
                val message = when {
                    added.isNotEmpty() -> text(R.string.msg_volume_connected, added.first().label)
                    removed.isNotEmpty() -> text(R.string.msg_volume_removed, removed.first().label)
                    else -> null
                }
                if (message != null) _state.update { it.copy(message = message) }
            }
        }
    }

    /** True jika [path] bisa dibuka tanpa root: penyimpanan internal, flashdisk, atau kartu SD. */
    private fun isUserStorage(path: String): Boolean =
        path.startsWith(FileRepository.STORAGE_ROOT) || Volumes.isDirect(path)

    /**
     * Folder awal satu panel: jalur startup panel itu ([sideKey]) jika diisi dan masih bisa dibuka,
     * lalu beranda pilihan user, dan terakhir penyimpanan internal.
     */
    private fun startFolder(sideKey: String): String {
        for (key in listOf(sideKey, AppPrefs.KEY_HOME)) {
            val path = prefs.getString(key, "").orEmpty()
            if (path.startsWith(FileRepository.STORAGE_ROOT) && File(path).isDirectory) return path
        }
        return FileRepository.STORAGE_ROOT
    }

    /** Menu titik tiga "Atur sebagai beranda": folder ini dibuka setiap aplikasi mulai. */
    fun setHome(path: String) {
        if (!path.startsWith(FileRepository.STORAGE_ROOT)) {
            _state.update { it.copy(message = text(R.string.msg_home_storage_only)) }
            return
        }
        prefs.edit().putString(AppPrefs.KEY_HOME, path).apply()
        _state.update { it.copy(message = text(R.string.msg_home_set)) }
    }

    /** Menampilkan pesan singkat di bawah layar. */
    fun notify(message: String) {
        _state.update { it.copy(message = message) }
    }

    /** Menu samping "Direktori root": membuka "/". Akses root diminta dulu jika belum aktif. */
    fun openRootDir() {
        if (_state.value.rootMode) {
            openFolder(_state.value.active, "/")
            return
        }
        viewModelScope.launch {
            val granted = withContext(Dispatchers.IO) { RootShell.isGranted() }
            if (granted) {
                _state.update { it.copy(rootMode = true) }
                openFolder(_state.value.active, "/")
                reloadAll()
            } else {
                _state.update { it.copy(message = text(R.string.msg_root_denied)) }
            }
        }
    }

    private fun text(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    // ---------- Panel dan navigasi ----------

    fun reloadAll() {
        loadPanel(0)
        loadPanel(1)
    }

    fun loadPanel(index: Int) {
        val path = _state.value.panels[index].path
        val root = _state.value.rootMode
        updatePanel(index) { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                // Penyimpanan internal dibaca langsung karena lebih cepat. Root dipakai untuk folder lain,
                // dan untuk Android/data serta Android/obb yang disembunyikan Android dari aplikasi biasa.
                // Flashdisk dan kartu SD di /storage juga dibaca langsung.
                val inStorage = path.startsWith("/storage/emulated") || Volumes.isDirect(path)
                // Berkas arsip (ZIP, JAR, APK) dibuka seperti folder
                val archive = Archive.locate(path)
                when {
                    archive != null -> Archive.list(archive)
                    root && FileRepository.isProtectedStorage(path) -> FileRepository.listProtected(path)
                    root && !inStorage -> FileRepository.listRoot(path)
                    else -> FileRepository.listLocal(path)
                }
            }
            val sort = _state.value.sort
            updatePanel(index) { panel ->
                if (panel.path != path) {
                    panel // folder sudah berganti, hasil ini dibuang
                } else {
                    result.fold(
                        onSuccess = { list ->
                            panel.copy(loading = false, error = false, items = sortItems(list, sort))
                        },
                        onFailure = {
                            panel.copy(loading = false, error = true, items = emptyList())
                        },
                    )
                }
            }
        }
    }

    fun setActive(index: Int) {
        _state.update { s ->
            s.copy(
                active = index,
                panels = s.panels.mapIndexed { i, p -> if (i == index) p else p.copy(selected = emptySet()) },
            )
        }
    }

    /** Membuka folder. Folder yang ditinggalkan masuk riwayat, supaya bisa kembali. */
    fun openFolder(index: Int, path: String) {
        _state.update { s ->
            s.copy(
                active = index,
                panels = s.panels.mapIndexed { i, p ->
                    when {
                        i != index -> p.copy(selected = emptySet())
                        p.path == path -> p.copy(selected = emptySet())
                        else -> p.copy(
                            path = path,
                            items = emptyList(),
                            selected = emptySet(),
                            back = (p.back + p.path).takeLast(MAX_HISTORY),
                            forward = emptyList(),
                        )
                    }
                },
            )
        }
        loadPanel(index)
    }

    /** Tombol kembali di bilah bawah: pindah ke folder sebelumnya. */
    fun historyBack(index: Int) {
        val target = _state.value.panels[index].back.lastOrNull() ?: return
        moveInHistory(index, target, goingBack = true)
    }

    /** Tombol maju di bilah bawah: kembali ke folder yang tadi ditinggalkan. */
    fun historyForward(index: Int) {
        val target = _state.value.panels[index].forward.lastOrNull() ?: return
        moveInHistory(index, target, goingBack = false)
    }

    private fun moveInHistory(index: Int, target: String, goingBack: Boolean) {
        _state.update { s ->
            s.copy(
                active = index,
                panels = s.panels.mapIndexed { i, p ->
                    when {
                        i != index -> p.copy(selected = emptySet())
                        goingBack -> p.copy(
                            path = target,
                            items = emptyList(),
                            selected = emptySet(),
                            back = p.back.dropLast(1),
                            forward = (p.forward + p.path).takeLast(MAX_HISTORY),
                        )
                        else -> p.copy(
                            path = target,
                            items = emptyList(),
                            selected = emptySet(),
                            back = (p.back + p.path).takeLast(MAX_HISTORY),
                            forward = p.forward.dropLast(1),
                        )
                    }
                },
            )
        }
        loadPanel(index)
    }

    /** Menukar isi dua panel. Hanya berlaku saat mode dua panel aktif. */
    fun swapPanels() {
        if (!_state.value.dual) return
        _state.update { s -> s.copy(panels = s.panels.reversed(), active = 1 - s.active) }
        // Muat ulang supaya status "sedang memuat" kedua panel tidak tertinggal
        reloadAll()
    }

    /** Tombol sinkron di bilah bawah: panel sebelah membuka folder yang sama dengan panel aktif. */
    fun syncOtherPanel() {
        val s = _state.value
        if (!s.dual) return
        val current = s.active
        openFolder(1 - current, s.panels[current].path)
        _state.update { it.copy(active = current) }
    }

    /** Tombol naik di bilah bawah: membuka folder induk. */
    fun goUp() {
        val s = _state.value
        parentFolder(s.panels[s.active].path)?.let { openFolder(s.active, it) }
    }

    /** Dipanggil tombol kembali. Mengembalikan false jika tidak ada yang bisa dilakukan. */
    fun goBack(): Boolean {
        val s = _state.value
        if (s.searchOpen) {
            toggleSearch(false)
            return true
        }
        if (s.panels.any { it.selected.isNotEmpty() }) {
            clearSelection()
            return true
        }
        val parent = parentFolder(s.panels[s.active].path) ?: return false
        openFolder(s.active, parent)
        return true
    }

    /** Folder induk untuk baris "..". Null jika sudah di batas (tanpa root, batasnya penyimpanan pengguna). */
    fun parentFolder(path: String): String? {
        if (path == "/" || path.isEmpty()) return null
        val parent = FileRepository.parentOf(path)
        // Tanpa root, naik paling jauh sampai /storage/emulated (isinya folder "0"), seperti NP Manager
        if (!_state.value.rootMode) {
            // Di flashdisk atau kartu SD, batasnya folder paling atas penyimpanan itu
            val volume = Volumes.find(path)
            if (volume != null) return if (path == volume.path) null else parent
            if (!parent.startsWith("/storage/emulated")) return null
        }
        return parent
    }

    // ---------- Pilihan ----------

    fun toggleSelect(index: Int, path: String) {
        _state.update { s ->
            s.copy(
                active = index,
                panels = s.panels.mapIndexed { i, p ->
                    when {
                        i == index -> {
                            val selected = if (path in p.selected) p.selected - path else p.selected + path
                            p.copy(selected = selected)
                        }
                        p.selected.isEmpty() -> p
                        else -> p.copy(selected = emptySet())
                    }
                },
            )
        }
    }

    /** Memilih semua berkas yang sedang tampil di panel [index]. */
    fun selectAll(index: Int) {
        _state.update { s ->
            s.copy(
                active = index,
                panels = s.panels.mapIndexed { i, p ->
                    if (i == index) {
                        val shown = visibleItems(p.items, s.search, s.showHidden, s.typeFilter)
                        p.copy(selected = shown.map { it.path }.toSet())
                    } else {
                        p.copy(selected = emptySet())
                    }
                },
            )
        }
    }

    /** Membalik pilihan: yang dipilih jadi tidak, yang tidak dipilih jadi dipilih. */
    fun invertSelection(index: Int) {
        _state.update { s ->
            s.copy(
                panels = s.panels.mapIndexed { i, p ->
                    if (i != index) {
                        p
                    } else {
                        val shown = visibleItems(p.items, s.search, s.showHidden, s.typeFilter)
                        p.copy(selected = shown.map { it.path }.filterNot { it in p.selected }.toSet())
                    }
                },
            )
        }
    }

    /**
     * Pilih rentang: semua berkas di antara berkas pertama dan terakhir yang dipilih ikut dipilih.
     * Mengembalikan false jika yang dipilih belum sampai dua.
     */
    fun selectRange(index: Int): Boolean {
        val s = _state.value
        val p = s.panels[index]
        val shown = visibleItems(p.items, s.search, s.showHidden, s.typeFilter)
        val marks = shown.indices.filter { shown[it].path in p.selected }
        if (marks.size < 2) return false
        val range = shown.subList(marks.first(), marks.last() + 1).map { it.path }.toSet()
        updatePanel(index) { it.copy(selected = range) }
        return true
    }

    /** Memilih semua berkas yang jenisnya sama dengan yang sedang dipilih (folder dengan folder, akhiran dengan akhiran). */
    fun selectSameType(index: Int) {
        val s = _state.value
        val p = s.panels[index]
        val shown = visibleItems(p.items, s.search, s.showHidden, s.typeFilter)
        val kinds = shown.filter { it.path in p.selected }.map { if (it.isDir) "/" else it.extension }.toSet()
        val same = shown.filter { (if (it.isDir) "/" else it.extension) in kinds }.map { it.path }.toSet()
        updatePanel(index) { it.copy(selected = same) }
    }

    fun clearSelection() {
        _state.update { s -> s.copy(panels = s.panels.map { it.copy(selected = emptySet()) }) }
    }

    private fun selectedPaths(): List<String> {
        val s = _state.value
        return s.panels[s.active].selected.toList()
    }

    // ---------- Cari, urutkan, filter, berkas tersembunyi ----------

    fun toggleSearch(open: Boolean) {
        _state.update { s -> s.copy(searchOpen = open, search = if (open) s.search else "") }
    }

    fun setSearch(value: String) {
        _state.update { it.copy(search = value) }
    }

    fun setSort(mode: SortMode) {
        _state.update { s ->
            s.copy(sort = mode, panels = s.panels.map { it.copy(items = sortItems(it.items, mode)) })
        }
    }

    fun setTypeFilter(filter: TypeFilter) {
        _state.update { it.copy(typeFilter = filter) }
    }

    fun toggleHidden() {
        _state.update { it.copy(showHidden = !it.showHidden) }
    }

    fun setDual(enabled: Boolean) {
        _state.update { it.copy(dual = enabled) }
        // Muat ulang kedua panel, supaya panel yang gagal dimuat saat aplikasi dibuka bisa dipakai lagi
        if (enabled) reloadAll()
    }

    /** [quiet] dipakai saat aplikasi mulai, supaya tidak muncul pesan jika root tidak tersedia. */
    fun setRootMode(enabled: Boolean, quiet: Boolean = false) {
        if (!enabled) {
            _state.update { s ->
                s.copy(
                    rootMode = false,
                    message = text(R.string.msg_root_off),
                    panels = s.panels.map { p ->
                        val inStorage = isUserStorage(p.path)
                        p.copy(
                            path = if (inStorage) p.path else FileRepository.STORAGE_ROOT,
                            selected = if (inStorage) p.selected else emptySet(),
                            // Riwayat yang berisi folder root (misalnya / atau /storage) dibuang,
                            // karena folder itu tidak bisa dibuka tanpa akses root
                            back = p.back.filter { isUserStorage(it) },
                            forward = p.forward.filter { isUserStorage(it) },
                        )
                    },
                )
            }
            reloadAll()
            return
        }
        viewModelScope.launch {
            val granted = withContext(Dispatchers.IO) { RootShell.isGranted() }
            if (granted) {
                _state.update { it.copy(rootMode = true, message = if (quiet) null else text(R.string.msg_root_on)) }
                reloadAll()
                // Dengan root, flashdisk yang tidak ditampilkan ke aplikasi biasa ikut dicari
                refreshVolumes()
            } else if (!quiet) {
                _state.update { it.copy(message = text(R.string.msg_root_denied)) }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    // ---------- Bookmark ----------

    fun addBookmark(path: String) {
        val current = _state.value.bookmarks
        if (path in current) {
            _state.update { it.copy(message = text(R.string.msg_bookmark_exists)) }
            return
        }
        val updated = current + path
        saveBookmarks(updated)
        _state.update { it.copy(bookmarks = updated, message = text(R.string.msg_bookmark_added)) }
    }

    fun removeBookmark(path: String) {
        val updated = _state.value.bookmarks - path
        saveBookmarks(updated)
        _state.update { it.copy(bookmarks = updated) }
    }

    private fun loadBookmarks(): List<String> =
        prefs.getString(AppPrefs.KEY_BOOKMARKS, "").orEmpty().lines().filter { it.isNotBlank() }

    private fun saveBookmarks(list: List<String>) {
        prefs.edit().putString(AppPrefs.KEY_BOOKMARKS, list.joinToString("\n")).apply()
    }

    // ---------- Dialog dan tindakan ----------

    fun showDialog(dialog: AppDialog) {
        _state.update { it.copy(dialog = dialog) }
    }

    fun dismissDialog() {
        _state.update { it.copy(dialog = null) }
    }

    /** Menjalankan tindakan. Jika menyentuh folder sistem atau menghapus, minta konfirmasi dulu. */
    fun requestAction(action: FileAction, confirm: Boolean = false) {
        // Isi arsip hanya bisa dibaca dan disalin keluar
        if (blockedByArchive(action)) {
            _state.update { it.copy(dialog = null, message = text(R.string.msg_archive_readonly)) }
            return
        }
        val system = involvesSystem(action)
        val needsConfirm = system || confirm || action is FileAction.Delete
        if (needsConfirm) {
            _state.update { it.copy(dialog = AppDialog.Confirm(action, system)) }
        } else {
            _state.update { it.copy(dialog = null) }
            execute(action)
        }
    }

    fun confirmAction(action: FileAction) {
        _state.update { it.copy(dialog = null) }
        execute(action)
    }

    fun deleteSelected() {
        deletePaths(selectedPaths())
    }

    /** Menghapus berkas tertentu (misalnya dari menu tekan lama). */
    fun deletePaths(paths: List<String>) {
        if (paths.isNotEmpty()) requestAction(FileAction.Delete(paths))
    }

    fun setClipFromSelection(move: Boolean) {
        setClipPaths(selectedPaths(), move)
    }

    fun setClipPaths(paths: List<String>, move: Boolean) {
        if (paths.isEmpty()) return
        _state.update { it.copy(clip = Clip(paths, move), message = text(R.string.msg_clip_ready)) }
        clearSelection()
    }

    /** Menyalin atau memindahkan berkas langsung ke folder panel sebelahnya (hanya mode dua panel). */
    fun transferToOtherPanel(fromIndex: Int, paths: List<String>, move: Boolean) {
        val s = _state.value
        if (!s.dual || paths.isEmpty()) return
        val dest = s.panels[1 - fromIndex].path
        // Seperti MT Manager, salin dan pindah ke panel sebelah ditanyakan dulu
        requestAction(FileAction.Paste(paths, move, dest), confirm = true)
    }

    /** Menyalin atau memindahkan berkas yang dipilih langsung ke panel sebelahnya. */
    fun transferSelectionToOther(move: Boolean) {
        transferToOtherPanel(_state.value.active, selectedPaths(), move)
    }

    fun pasteHere() {
        val s = _state.value
        val clip = s.clip ?: return
        val dest = s.panels[s.active].path
        requestAction(FileAction.Paste(clip.paths, clip.move, dest))
    }

    /** True jika tindakan ini mencoba mengubah isi arsip, yang tidak didukung. */
    private fun blockedByArchive(action: FileAction): Boolean = when (action) {
        is FileAction.Delete -> action.paths.any { Archive.inside(it) }
        is FileAction.Paste -> Archive.locate(action.destDir) != null ||
            (action.move && action.sources.any { Archive.inside(it) })
        is FileAction.Rename -> Archive.inside(action.path)
        is FileAction.NewFolder -> Archive.locate(action.parentDir) != null
        is FileAction.NewFile -> Archive.locate(action.parentDir) != null
    }

    private fun involvesSystem(action: FileAction): Boolean = when (action) {
        is FileAction.Delete -> action.paths.any { FileRepository.isSystemPath(it) }
        is FileAction.Paste -> FileRepository.isSystemPath(action.destDir) ||
            action.sources.any { FileRepository.isSystemPath(it) }
        is FileAction.Rename -> FileRepository.isSystemPath(action.path)
        is FileAction.NewFolder -> FileRepository.isSystemPath(action.parentDir)
        is FileAction.NewFile -> FileRepository.isSystemPath(action.parentDir)
    }

    private fun execute(action: FileAction) {
        val root = _state.value.rootMode
        viewModelScope.launch {
            pasted = emptySet()
            val (ok, message) = withContext(Dispatchers.IO) { runOperation(action, root) }
            _state.update { s ->
                s.copy(
                    message = message,
                    fresh = if (action is FileAction.Paste) pasted else s.fresh,
                    // Clip hanya dihapus jika yang ditempel memang isi clip, bukan transfer langsung ke panel sebelah
                    clip = if (action is FileAction.Paste && ok && s.clip?.paths == action.sources) null else s.clip,
                )
            }
            clearSelection()
            reloadAll()
        }
    }

    // Alamat hasil salin atau pindah yang terakhir, diisi oleh runOperation
    @Volatile
    private var pasted: Set<String> = emptySet()

    private fun runOperation(action: FileAction, root: Boolean): Pair<Boolean, String> {
        return when (action) {
            is FileAction.Delete -> {
                // Recycle Bin hanya untuk berkas di penyimpanan internal. Selain itu dihapus selamanya.
                val useTrash = action.toTrash && action.paths.all { Trash.supports(it) }
                val ok = if (useTrash) {
                    action.paths.map { Trash.move(it) }.all { it }
                } else {
                    FileRepository.delete(action.paths, root)
                }
                val msg = when {
                    !ok -> text(R.string.msg_failed)
                    useTrash -> text(R.string.msg_trashed, action.paths.size)
                    else -> text(R.string.msg_deleted, action.paths.size)
                }
                Pair(ok, msg)
            }

            is FileAction.NewFile -> {
                val name = action.name.trim()
                val target = FileRepository.join(action.parentDir, name)
                when {
                    !isValidName(name) -> Pair(false, text(R.string.msg_invalid_name))
                    FileRepository.exists(target, root) -> Pair(false, text(R.string.msg_name_exists))
                    else -> {
                        val ok = FileRepository.createFile(action.parentDir, name, root)
                        Pair(ok, text(if (ok) R.string.msg_file_created else R.string.msg_failed))
                    }
                }
            }

            is FileAction.Paste -> {
                val made = HashSet<String>()
                val results = action.sources.map { src ->
                    val intoItself = action.destDir == src || action.destDir.startsWith("$src/")
                    val sameSpot = action.move && FileRepository.parentOf(src) == action.destDir
                    val inArchive = Archive.locate(src)?.takeIf { it.inner.isNotEmpty() }
                    when {
                        intoItself -> false
                        sameSpot -> true
                        // Berkas di dalam arsip dikeluarkan ke folder tujuan
                        inArchive != null -> {
                            val out = Archive.extract(inArchive, File(action.destDir))
                            if (out != null) made.add(out.path)
                            out != null
                        }
                        else -> {
                            val target = FileRepository.transfer(src, action.destDir, action.move, root)
                            if (target != null) made.add(target)
                            target != null
                        }
                    }
                }
                pasted = made
                val good = results.count { it }
                Pair(good == results.size, text(R.string.msg_paste_result, good, results.size))
            }

            is FileAction.Rename -> {
                val name = action.newName.trim()
                val target = FileRepository.join(FileRepository.parentOf(action.path), name)
                when {
                    !isValidName(name) -> Pair(false, text(R.string.msg_invalid_name))
                    FileRepository.exists(target, root) -> Pair(false, text(R.string.msg_name_exists))
                    else -> {
                        val ok = FileRepository.rename(action.path, name, root)
                        Pair(ok, text(if (ok) R.string.msg_renamed else R.string.msg_failed))
                    }
                }
            }

            is FileAction.NewFolder -> {
                val name = action.name.trim()
                val target = FileRepository.join(action.parentDir, name)
                when {
                    !isValidName(name) -> Pair(false, text(R.string.msg_invalid_name))
                    FileRepository.exists(target, root) -> Pair(false, text(R.string.msg_name_exists))
                    else -> {
                        val ok = FileRepository.createFolder(action.parentDir, name, root)
                        Pair(ok, text(if (ok) R.string.msg_folder_created else R.string.msg_failed))
                    }
                }
            }
        }
    }

    private fun isValidName(name: String): Boolean =
        name.isNotBlank() && !name.contains('/') && name != "." && name != ".."

    // ---------- Kompres dan ekstrak ZIP ----------

    /** Mengompres berkas atau folder menjadi ZIP di folder yang sama. */
    /** Mengompres beberapa berkas yang dipilih ke satu ZIP, diberi nama seperti folder tempatnya. */
    fun compressPaths(paths: List<String>) {
        if (paths.size == 1) {
            compressPath(paths.first())
            return
        }
        val sources = paths.map { File(it) }
        val dir = sources.firstOrNull()?.parentFile ?: return
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                val dest = uniqueFile(dir, dir.name.ifEmpty { "Arsip" }, ".zip")
                if (Archive.zipAll(sources, dest)) {
                    text(R.string.msg_compressed, dest.name)
                } else {
                    text(R.string.msg_compress_failed)
                }
            }
            _state.update { it.copy(message = message) }
            reloadAll()
        }
    }

    fun compressPath(path: String) {
        val source = File(path)
        val dir = source.parentFile ?: return
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                val dest = uniqueFile(dir, source.name, ".zip")
                if (Archive.zip(source, dest)) {
                    text(R.string.msg_compressed, dest.name)
                } else {
                    text(R.string.msg_compress_failed)
                }
            }
            _state.update { it.copy(message = message) }
            reloadAll()
        }
    }

    /**
     * Mengekstrak arsip [path] ke folder [targetDir]. Jika [deleteSource] true, arsipnya dihapus
     * setelah berhasil diekstrak.
     */
    fun extractZip(path: String, targetDir: String, deleteSource: Boolean) {
        val zip = File(path)
        val target = File(targetDir)
        _state.update { it.copy(dialog = null) }
        if (Archive.locate(targetDir) != null) {
            _state.update { it.copy(message = text(R.string.msg_archive_readonly)) }
            return
        }
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                if (Archive.unzip(zip, target)) {
                    if (deleteSource) zip.delete()
                    text(R.string.msg_extracted, target.name.ifEmpty { target.path })
                } else {
                    text(R.string.msg_extract_failed)
                }
            }
            _state.update { it.copy(message = message) }
            reloadAll()
        }
    }

    // ---------- Kunci tanda tangan ----------

    /** "Importir tanda tangan": menyalin berkas kunci (dan sertifikat pasangannya) ke folder kunci Filemate. */
    fun importKey(path: String) = runTool {
        val source = File(path)
        val dir = File(FileRepository.FILEMATE_DIR, "keys")
        dir.mkdirs()
        var copied = 0
        val base = source.path.removeSuffix("." + source.extension)
        // Berkas pk8 butuh sertifikat pasangannya, jadi keduanya ikut disalin jika ada
        val family = listOf(source) + listOf("pk8", "x509.pem", "pem", "crt", "cer").map { File("$base.$it") }
        for (file in family.distinctBy { it.path }) {
            if (file.isFile) {
                file.copyTo(File(dir, file.name), overwrite = true)
                copied++
            }
        }
        if (copied > 0) text(R.string.msg_key_imported, dir.path) else text(R.string.msg_failed)
    }

    /**
     * Memeriksa APK yang akan dipasang terhadap aplikasi yang sudah terpasang: versi lebih lama
     * atau tanda tangan berbeda akan membuat pemasangan gagal. Mengembalikan daftar peringatan.
     */
    fun installWarnings(): List<String> {
        val target = installTarget() ?: return emptyList()
        val pm = getApplication<Application>().packageManager
        return try {
            val archive = pm.getPackageArchiveInfo(target, 0) ?: return emptyList()
            val installed = try {
                pm.getPackageInfo(archive.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } catch (e: PackageManager.NameNotFoundException) {
                return emptyList()
            }
            val warnings = ArrayList<String>()
            if (archive.longVersionCode < installed.longVersionCode) {
                warnings.add(text(R.string.install_warn_downgrade, archive.longVersionCode, installed.longVersionCode))
            }
            val current = installed.signingInfo?.apkContentsSigners.orEmpty().map { it.toByteArray().toList() }.toSet()
            val incoming = ApkTools.signerCertificates(File(target)).map { it.toList() }.toSet()
            if (current.isNotEmpty() && incoming.isNotEmpty() && current.intersect(incoming).isEmpty()) {
                warnings.add(text(R.string.install_warn_signature))
            }
            warnings
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------- Peralatan berkas (menu tekan lama, "Peralatan") ----------

    /** Menjalankan pekerjaan di latar belakang, lalu menampilkan pesannya dan memuat ulang panel. */
    private fun runTool(work: () -> String) {
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                try {
                    work()
                } catch (e: Exception) {
                    text(R.string.msg_failed)
                }
            }
            _state.update { it.copy(message = message) }
            clearSelection()
            reloadAll()
        }
    }

    fun splitFile(path: String, partMegabytes: Int) = runTool {
        val count = FileTools.split(File(path), partMegabytes.coerceAtLeast(1) * 1024L * 1024L)
        text(R.string.msg_split_done, count)
    }

    fun joinParts(path: String) = runTool {
        val out = FileTools.join(File(path))
        if (out == null) text(R.string.msg_join_no_first) else text(R.string.msg_join_done, out.name)
    }

    fun encryptFile(path: String, password: String) = runTool {
        val src = File(path)
        val dest = FileTools.uniqueFile(src.parentFile ?: File("/"), src.name + FileTools.ENCRYPTED_EXT)
        FileTools.encrypt(src, dest, password.toCharArray())
        text(R.string.msg_encrypt_done, dest.name)
    }

    fun decryptFile(path: String, password: String) = runTool {
        val src = File(path)
        val dest = FileTools.uniqueFile(src.parentFile ?: File("/"), src.name.removeSuffix(FileTools.ENCRYPTED_EXT))
        if (FileTools.decrypt(src, dest, password.toCharArray())) {
            text(R.string.msg_decrypt_done, dest.name)
        } else {
            text(R.string.msg_decrypt_wrong)
        }
    }

    fun swapNames(first: String, second: String) = runTool {
        text(if (FileTools.swapNames(File(first), File(second))) R.string.msg_swap_done else R.string.msg_failed)
    }

    /**
     * Membuat tautan lunak ke [path]. Dalam mode dua panel tautan dibuat di folder panel sebelah,
     * kalau tidak di folder yang sama. Penyimpanan internal biasanya menolak tautan, jadi root dicoba juga.
     */
    fun makeSymlink(path: String) {
        val s = _state.value
        val other = s.panels[1 - s.active].path
        val here = FileRepository.parentOf(path)
        val destDir = if (s.dual && other != here) other else here
        val root = s.rootMode
        runTool {
            val name = FileRepository.nameOf(path).let { if (destDir == here) "$it-tautan" else it }
            val link = FileRepository.join(destDir, name)
            val direct = try {
                android.system.Os.symlink(path, link)
                true
            } catch (e: Exception) {
                false
            }
            val ok = direct ||
                (root && RootShell.run("ln -s ${RootShell.quote(path)} ${RootShell.quote(link)}").exitCode == 0)
            text(if (ok) R.string.msg_symlink_done else R.string.msg_symlink_failed)
        }
    }

    /** Menulis daftar isi [paths] ke berkas teks di folder yang sedang dibuka. */
    fun exportFileList(paths: List<String>) {
        val s = _state.value
        val dir = File(s.panels[s.active].path)
        runTool {
            val out = FileTools.uniqueFile(dir, "daftar-berkas.txt")
            val count = FileTools.exportList(paths.map { File(it) }, out)
            text(R.string.msg_export_done, count, out.name)
        }
    }

    /** Nama yang belum dipakai di [dir]: "nama", lalu "nama (2)", "nama (3)", dan seterusnya. */
    private fun uniqueFile(dir: File, base: String, ext: String): File {
        var candidate = File(dir, base + ext)
        var n = 2
        while (candidate.exists()) {
            candidate = File(dir, "$base ($n)$ext")
            n++
        }
        return candidate
    }

    // ---------- Alat APK ----------

    fun openApk(path: String) {
        _state.update { it.copy(dialog = AppDialog.Apk(path), apk = ApkInfo(path = path, busy = true)) }
        viewModelScope.launch {
            val manifest = withContext(Dispatchers.IO) {
                try {
                    ApkTools.readManifest(ensureReadable(path)).take(MAX_MANIFEST_CHARS)
                } catch (e: Exception) {
                    text(R.string.apk_manifest_failed)
                }
            }
            updateApk { it.copy(busy = false, manifest = manifest) }
        }
    }

    fun dismissApk() {
        _state.update { it.copy(dialog = null, apk = null) }
    }

    fun unpackApk() {
        val path = _state.value.apk?.path ?: return
        val base = baseName(path)
        updateApk { it.copy(busy = true, status = "") }
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) {
                try {
                    val out = File(FileRepository.FILEMATE_DIR, base)
                    ApkTools.unpack(ensureReadable(path), out)
                    text(R.string.msg_apk_unpacked, out.path)
                } catch (e: Exception) {
                    text(R.string.msg_apk_failed)
                }
            }
            updateApk { it.copy(busy = false, status = status) }
            reloadAll()
        }
    }

    fun repackApk() {
        val path = _state.value.apk?.path ?: return
        val base = baseName(path)
        val folder = File(FileRepository.FILEMATE_DIR, base)
        updateApk { it.copy(busy = true, status = "") }
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) {
                if (!folder.isDirectory) {
                    text(R.string.msg_need_unpack)
                } else {
                    try {
                        val out = File(FileRepository.FILEMATE_DIR, "$base-rakit.apk")
                        ApkTools.repack(folder, out)
                        // Pengaturan "Tanda tangan otomatis": hasil rakit langsung ditandatangani
                        if (prefs.getBoolean(AppPrefs.KEY_AUTO_SIGN, true)) {
                            val signed = File(FileRepository.FILEMATE_DIR, "$base-ttd.apk")
                            ApkTools.sign(out, signed)
                            text(R.string.msg_apk_signed, signed.path)
                        } else {
                            text(R.string.msg_apk_repacked, out.path)
                        }
                    } catch (e: Exception) {
                        text(R.string.msg_apk_failed)
                    }
                }
            }
            updateApk { it.copy(busy = false, status = status) }
            reloadAll()
        }
    }

    /** Menandatangani hasil rakit ulang jika ada. Jika tidak ada, APK asli yang ditandatangani. */
    fun signApk() {
        val path = _state.value.apk?.path ?: return
        val base = baseName(path)
        val repacked = File(FileRepository.FILEMATE_DIR, "$base-rakit.apk")
        updateApk { it.copy(busy = true, status = "") }
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) {
                try {
                    val source = if (repacked.isFile) repacked else ensureReadable(path)
                    val out = File(FileRepository.FILEMATE_DIR, "$base-ttd.apk")
                    ApkTools.sign(source, out)
                    text(R.string.msg_apk_signed, out.path)
                } catch (e: Exception) {
                    text(R.string.msg_apk_failed)
                }
            }
            updateApk { it.copy(busy = false, status = status) }
            reloadAll()
        }
    }

    /** APK yang akan dipasang: hasil tanda tangan jika ada, kalau tidak APK yang sedang dibuka. */
    fun installTarget(): String? {
        val path = _state.value.apk?.path ?: return null
        val signed = File(FileRepository.FILEMATE_DIR, "${baseName(path)}-ttd.apk")
        return if (signed.isFile) signed.path else path
    }

    /** True jika pemasangan harus lewat root (pengaturan dinyalakan dan root aktif). */
    fun installsWithRoot(): Boolean =
        _state.value.rootMode && prefs.getBoolean(AppPrefs.KEY_ROOT_INSTALL, true)

    /** Memasang APK lewat root dengan perintah pm, tanpa layar pemasang Android. */
    fun installApkRoot() {
        val target = installTarget() ?: return
        val installer = prefs.getString(AppPrefs.KEY_INSTALLER, "").orEmpty().trim()
        updateApk { it.copy(busy = true, status = "") }
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) {
                // pm tidak selalu bisa membaca penyimpanan internal, jadi APK disalin dulu ke folder sementara sistem
                val temp = "/data/local/tmp/filemate_install.apk"
                val source = RootShell.quote(target)
                // Nama paket sumber hanya boleh huruf, angka, titik, dan garis bawah
                val from = if (installer.matches(Regex("[A-Za-z0-9_.]+"))) "-i $installer " else ""
                val result = RootShell.run(
                    "cp $source $temp && chmod 644 $temp && pm install -r $from$temp; code=\$?; rm -f $temp; exit \$code",
                )
                if (result.exitCode == 0) {
                    text(R.string.msg_apk_installed)
                } else {
                    text(R.string.msg_apk_install_failed, result.output.trim().takeLast(200))
                }
            }
            updateApk { it.copy(busy = false, status = status) }
        }
    }

    private fun updateApk(transform: (ApkInfo) -> ApkInfo) {
        _state.update { s -> s.copy(apk = s.apk?.let(transform)) }
    }

    /**
     * Berkas di folder sistem tidak bisa dibaca aplikasi biasa.
     * Dalam mode root, berkas disalin dulu ke cache aplikasi.
     */
    private fun ensureReadable(path: String): File {
        val original = File(path)
        if (original.canRead()) return original
        val copy = File(getApplication<Application>().cacheDir, "salinan_${System.currentTimeMillis()}.apk")
        val src = RootShell.quote(path)
        val dst = RootShell.quote(copy.path)
        val result = RootShell.run("cp $src $dst && chmod 644 $dst")
        check(result.exitCode == 0) { result.output }
        return copy
    }

    private fun baseName(path: String): String = File(path).nameWithoutExtension

    // ---------- Bantuan ----------

    private fun updatePanel(index: Int, transform: (PanelState) -> PanelState) {
        _state.update { s ->
            s.copy(panels = s.panels.mapIndexed { i, p -> if (i == index) transform(p) else p })
        }
    }

    /** Folder dulu, lalu sesuai urutan yang dipilih. */
    private fun sortItems(list: List<FileItem>, mode: SortMode): List<FileItem> {
        val byMode: Comparator<FileItem> = when (mode) {
            SortMode.NAME -> compareBy<FileItem> { it.name.lowercase() }
            SortMode.SIZE -> compareByDescending<FileItem> { it.size }.thenBy { it.name.lowercase() }
            SortMode.DATE -> compareByDescending<FileItem> { it.modified }
        }
        return list.sortedWith(compareByDescending<FileItem> { it.isDir }.then(byMode))
    }

    companion object {
        private const val MAX_MANIFEST_CHARS = 20000
        private const val MAX_HISTORY = 50
    }
}
