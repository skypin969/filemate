package com.filemate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filemate.app.R
import com.filemate.app.apk.ApkTools
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import com.filemate.app.data.RootShell
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode { NAME, SIZE, DATE }

/** Keadaan satu panel: folder yang dibuka, isinya, dan item yang dipilih. */
data class PanelState(
    val path: String,
    val items: List<FileItem> = emptyList(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val selected: Set<String> = emptySet(),
)

/** Berkas yang sudah disalin atau dipotong, menunggu ditempel. */
data class Clip(val paths: List<String>, val move: Boolean)

/** Tindakan yang mengubah berkas. */
sealed interface FileAction {
    data class Delete(val paths: List<String>) : FileAction
    data class Paste(val sources: List<String>, val move: Boolean, val destDir: String) : FileAction
    data class Rename(val path: String, val newName: String) : FileAction
    data class NewFolder(val parentDir: String, val name: String) : FileAction
}

/** Dialog yang sedang tampil. */
sealed interface AppDialog {
    data class NewFolder(val parentDir: String) : AppDialog
    data class Rename(val path: String) : AppDialog
    data class Confirm(val action: FileAction, val system: Boolean) : AppDialog
    data class Apk(val path: String) : AppDialog
}

/** Keadaan dialog alat APK. */
data class ApkInfo(
    val path: String,
    val manifest: String = "",
    val busy: Boolean = false,
    val status: String = "",
)

data class UiState(
    val dual: Boolean = false,
    val rootMode: Boolean = false,
    val active: Int = 0,
    val panels: List<PanelState> = listOf(
        PanelState(FileRepository.STORAGE_ROOT + "/Download"),
        PanelState(FileRepository.STORAGE_ROOT + "/Documents"),
    ),
    val sort: SortMode = SortMode.NAME,
    val search: String = "",
    val searchOpen: Boolean = false,
    val clip: Clip? = null,
    val dialog: AppDialog? = null,
    val apk: ApkInfo? = null,
    val message: String? = null,
)

class FileManagerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        reloadAll()
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
                if (root) FileRepository.listRoot(path) else FileRepository.listLocal(path)
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

    fun openFolder(index: Int, path: String) {
        _state.update { s ->
            s.copy(
                active = index,
                panels = s.panels.mapIndexed { i, p ->
                    if (i == index) {
                        p.copy(path = path, items = emptyList(), selected = emptySet())
                    } else {
                        p.copy(selected = emptySet())
                    }
                },
            )
        }
        loadPanel(index)
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
        val parent = parentPath(s.panels[s.active].path) ?: return false
        openFolder(s.active, parent)
        return true
    }

    private fun parentPath(path: String): String? {
        if (path == "/" || path.isEmpty()) return null
        val s = _state.value
        val parent = FileRepository.parentOf(path)
        if (!s.rootMode) {
            // Tanpa root, navigasi berhenti di penyimpanan pengguna
            if (path == FileRepository.STORAGE_ROOT) return null
            if (!parent.startsWith(FileRepository.STORAGE_ROOT)) return null
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

    fun clearSelection() {
        _state.update { s -> s.copy(panels = s.panels.map { it.copy(selected = emptySet()) }) }
    }

    private fun selectedPaths(): List<String> {
        val s = _state.value
        return s.panels[s.active].selected.toList()
    }

    // ---------- Cari, urutkan, mode ----------

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

    fun setDual(enabled: Boolean) {
        _state.update { it.copy(dual = enabled) }
    }

    fun setRootMode(enabled: Boolean) {
        if (!enabled) {
            _state.update { s ->
                s.copy(
                    rootMode = false,
                    message = text(R.string.msg_root_off),
                    panels = s.panels.map { p ->
                        if (p.path.startsWith(FileRepository.STORAGE_ROOT)) {
                            p
                        } else {
                            p.copy(path = FileRepository.STORAGE_ROOT, selected = emptySet())
                        }
                    },
                )
            }
            reloadAll()
            return
        }
        viewModelScope.launch {
            val granted = withContext(Dispatchers.IO) { RootShell.isGranted() }
            if (granted) {
                _state.update { it.copy(rootMode = true, message = text(R.string.msg_root_on)) }
                reloadAll()
            } else {
                _state.update { it.copy(message = text(R.string.msg_root_denied)) }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
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
        val paths = selectedPaths()
        if (paths.isNotEmpty()) requestAction(FileAction.Delete(paths))
    }

    fun setClipFromSelection(move: Boolean) {
        val paths = selectedPaths()
        if (paths.isEmpty()) return
        _state.update { it.copy(clip = Clip(paths, move), message = text(R.string.msg_clip_ready)) }
        clearSelection()
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
    }

    private fun execute(action: FileAction) {
        val root = _state.value.rootMode
        viewModelScope.launch {
            val (ok, message) = withContext(Dispatchers.IO) { runOperation(action, root) }
            _state.update { s ->
                s.copy(
                    message = message,
                    clip = if (action is FileAction.Paste && ok) null else s.clip,
                )
            }
            clearSelection()
            reloadAll()
        }
    }

    private fun runOperation(action: FileAction, root: Boolean): Pair<Boolean, String> {
        return when (action) {
            is FileAction.Delete -> {
                val ok = FileRepository.delete(action.paths, root)
                val msg = if (ok) text(R.string.msg_deleted, action.paths.size) else text(R.string.msg_failed)
                Pair(ok, msg)
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
                        text(R.string.msg_apk_repacked, out.path)
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
    }
}
