package com.filemate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemate.app.R
import com.filemate.app.apk.ApkTools
import com.filemate.app.data.Archive
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
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
            panels = startFolder().let { listOf(PanelState(it), PanelState(it)) },
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        reloadAll()
        // Akses root dicoba otomatis setiap aplikasi dibuka. Jika izin sudah diberikan di aplikasi
        // pengelola root (Magisk, KernelSU), mode root langsung aktif tanpa tombol.
        setRootMode(true, quiet = true)
        // Sakelar "Mulai layanan secara otomatis saat aplikasi dibuka" di dialog APK MCP
        if (prefs.getBoolean(AppPrefs.KEY_MCP_AUTO, false)) {
            try {
                McpService.start(app)
            } catch (e: Exception) {
                // Android bisa menolak jika aplikasi belum tampil di layar. Layanan bisa dimulai manual.
            }
        }
    }

    /** Folder awal: folder beranda pilihan user jika masih bisa dibuka, kalau tidak penyimpanan internal. */
    private fun startFolder(): String {
        val home = prefs.getString(AppPrefs.KEY_HOME, "").orEmpty()
        val usable = home.startsWith(FileRepository.STORAGE_ROOT) && File(home).isDirectory
        return if (usable) home else FileRepository.STORAGE_ROOT
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
                val inStorage = path.startsWith("/storage/emulated")
                when {
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
        if (!_state.value.rootMode && !parent.startsWith("/storage/emulated")) return null
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
                        val inStorage = p.path.startsWith(FileRepository.STORAGE_ROOT)
                        p.copy(
                            path = if (inStorage) p.path else FileRepository.STORAGE_ROOT,
                            selected = if (inStorage) p.selected else emptySet(),
                            // Riwayat yang berisi folder root (misalnya / atau /storage) dibuang,
                            // karena folder itu tidak bisa dibuka tanpa akses root
                            back = p.back.filter { it.startsWith(FileRepository.STORAGE_ROOT) },
                            forward = p.forward.filter { it.startsWith(FileRepository.STORAGE_ROOT) },
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
    fun requestAction(action: FileAction) {
        val system = involvesSystem(action)
        val needsConfirm = system || action is FileAction.Delete
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
        requestAction(FileAction.Paste(paths, move, dest))
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
            val (ok, message) = withContext(Dispatchers.IO) { runOperation(action, root) }
            _state.update { s ->
                s.copy(
                    message = message,
                    // Clip hanya dihapus jika yang ditempel memang isi clip, bukan transfer langsung ke panel sebelah
                    clip = if (action is FileAction.Paste && ok && s.clip?.paths == action.sources) null else s.clip,
                )
            }
            clearSelection()
            reloadAll()
        }
    }

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
                val results = action.sources.map { src ->
                    val intoItself = action.destDir == src || action.destDir.startsWith("$src/")
                    val sameSpot = action.move && FileRepository.parentOf(src) == action.destDir
                    when {
                        intoItself -> false
                        sameSpot -> true
                        else -> FileRepository.transfer(src, action.destDir, action.move, root)
                    }
                }
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

    /** Mengekstrak ZIP ke folder baru dengan nama yang sama, di folder yang sama. */
    fun extractZip(path: String) {
        val zip = File(path)
        val dir = zip.parentFile ?: return
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                val target = uniqueFile(dir, zip.nameWithoutExtension, "")
                if (Archive.unzip(zip, target)) {
                    text(R.string.msg_extracted, target.name)
                } else {
                    // Hasil setengah jadi dibuang, supaya tidak ada folder kosong yang membingungkan
                    target.deleteRecursively()
                    text(R.string.msg_extract_failed)
                }
            }
            _state.update { it.copy(message = message) }
            reloadAll()
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
