package com.filemate.app.ui

import android.app.Activity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SyncAlt
import com.filemate.app.data.Archive
import androidx.compose.material.icons.filled.Usb
import com.filemate.app.data.Volumes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import android.widget.Toast
import android.net.Uri
import android.content.Intent
import android.content.Context
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filemate.app.R
import com.filemate.app.apk.Jar2Dex
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import com.filemate.app.data.RootShell
import com.filemate.app.data.Trash
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Layar utama, tampilannya mengikuti NP Manager: bilah atas biru berisi jalur folder,
 * dua panel berdampingan, dan lima tombol ikon di bawah.
 */
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenEditor: (EditorTarget) -> Unit,
    onOpenTerminal: (TerminalTarget) -> Unit,
    onOpenApps: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenTool: (ToolKind, String?) -> Unit,
    onOpenPreview: (PreviewTarget) -> Unit,
    vm: FileManagerViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    // Berkas yang sedang ditekan lama, beserta indeks panelnya. Null jika menu tidak tampil.
    var menuTarget by remember { mutableStateOf<Pair<Int, FileItem>?>(null) }
    var sortOpen by remember { mutableStateOf(false) }
    // Berkas yang sedang dipilihkan cara bukanya lewat dialog "Buka dengan"
    var openWithTarget by remember { mutableStateOf<FileItem?>(null) }
    val notReady = stringResource(R.string.msg_not_ready)
    val invalidLocal = stringResource(R.string.local_add_invalid)
    var mcpOpen by remember { mutableStateOf(false) }
    // Dialog "Lompat" yang muncul saat jalur di bilah atas diketuk
    var jumpOpen by remember { mutableStateOf(false) }
    // Pilihan tampilan dari halaman pengaturan. Dibaca ulang setiap kembali ke layar utama.
    val listPrefs = remember { AppPrefs.of(context) }
    val listStyle = remember {
        ListStyle(
            scale = when (listPrefs.getInt(AppPrefs.KEY_LIST_SIZE, 1)) {
                0 -> 0.88f
                2 -> 1.15f
                else -> 1f
            },
            nameLines = listPrefs.getInt(AppPrefs.KEY_NAME_LINES, 3).coerceIn(1, 8),
            thumbnails = listPrefs.getBoolean(AppPrefs.KEY_THUMBS, true),
            thumbMaxBytes = listPrefs.getInt(AppPrefs.KEY_THUMB_MAX_MB, 10).coerceIn(1, 200) * 1024L * 1024L,
            thumbTimeoutMs = listPrefs.getInt(AppPrefs.KEY_THUMB_TIMEOUT, 10).coerceIn(1, 120) * 1000L,
            showPerms = !listPrefs.getBoolean(AppPrefs.KEY_HIDE_PERMS, false),
            thumbsExternal = listPrefs.getBoolean(AppPrefs.KEY_THUMBS_EXTERNAL, true),
        )
    }
    // Pengaturan lain yang dibaca ulang setiap kembali ke layar utama
    val mdPreview = remember { listPrefs.getBoolean(AppPrefs.KEY_MD_PREVIEW, true) }
    val htmlPreview = remember { listPrefs.getBoolean(AppPrefs.KEY_HTML_PREVIEW, true) }
    val trashOn = remember { listPrefs.getBoolean(AppPrefs.KEY_TRASH_ON, true) }
    val trashDefault = remember { listPrefs.getBoolean(AppPrefs.KEY_TRASH_DEFAULT, true) }
    val deleteWarning = remember { listPrefs.getBoolean(AppPrefs.KEY_DELETE_WARNING, true) }
    val bottomMargin = remember { listPrefs.getInt(AppPrefs.KEY_BOTTOM_MARGIN, 0).coerceIn(0, 96) }
    // Dialog Layanan FTP, Layanan HTTP, atau Transfer LAN yang sedang terbuka
    var netDialog by remember { mutableStateOf<ToolKind?>(null) }
    // Dialog nama berkas baru untuk alat "Editor teks"
    var newTextOpen by remember { mutableStateOf(false) }
    // Daftar APK/DEX di folder ini untuk dipilih alat "Analisis kelas"
    var classPickOpen by remember { mutableStateOf(false) }
    val noApkHere = stringResource(R.string.dex_no_file)
    // Pengaturan menu samping: penyimpanan lokal tambahan, urutan dan alat yang disembunyikan,
    // serta dialog yang sedang terbuka dari menu samping
    var localPaths by remember { mutableStateOf(AppPrefs.localStorages(context)) }
    var toolOrderSaved by remember { mutableStateOf(AppPrefs.toolOrderText(context)) }
    var toolHiddenSaved by remember { mutableStateOf(AppPrefs.toolHiddenKeys(context)) }
    var drawerDialog by remember { mutableStateOf<DrawerDialog?>(null) }
    val jarWorking = stringResource(R.string.jar_working)
    val jarDone = stringResource(R.string.jar_done)
    val jarFailed = stringResource(R.string.jar_failed)
    val invalidPath = stringResource(R.string.msg_invalid_path)
    // Berkas yang sedang dibukakan dialog "Peralatan", dan alat yang masih menunggu isian (ukuran atau kata sandi)
    var toolTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var pendingTool by remember { mutableStateOf<Pair<FileTool, FileItem>?>(null) }
    val openFailed = stringResource(R.string.msg_cannot_open)
    val defaultSaved = stringResource(R.string.open_default_saved)
    // Peringatan yang harus disetujui sebelum APK dipasang. Null jika tidak ada dialog.
    var installAsk by remember { mutableStateOf<List<String>?>(null) }
    val menuOrder = remember { MenuOrder.load(context) }

    fun doInstall() {
        // Lewat root jika pengaturannya menyala dan root aktif, kalau tidak lewat pemasang Android (lihat startInstall)
        vm.startInstall()
    }
    val rangeHint = stringResource(R.string.msg_range_hint)
    val shortcutAsked = stringResource(R.string.msg_shortcut_asked)
    val shortcutFailed = stringResource(R.string.msg_shortcut_failed)
    val notReadyName = stringResource(R.string.msg_invalid_name)

    // Tombol kembali: tutup menu, lalu tutup pencarian/pilihan, lalu naik satu folder, lalu keluar
    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            !vm.goBack() -> (context as? Activity)?.finish()
        }
    }

    // Saat kembali dari editor atau pengaturan, isi panel dimuat ulang (ukuran dan tanggal berkas bisa berubah)
    LaunchedEffect(Unit) {
        vm.reloadAll()
    }

    // Pintasan folder di layar utama HP: buka folder yang diminta
    val pendingPath = PendingOpen.path
    LaunchedEffect(pendingPath) {
        if (pendingPath != null) {
            PendingOpen.path = null
            vm.openFolder(vm.state.value.active, pendingPath)
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            vm.consumeMessage()
            snackbar.showSnackbar(message)
        }
    }

    val active = state.panels[state.active]
    val selectedItems = active.items.filter { it.path in active.selected }
    val singleItem = selectedItems.size == 1
    val singleApk = singleItem && selectedItems.first().isApk

    // Baris keterangan kecil di bawah jalur: jumlah folder, jumlah berkas, pemakaian penyimpanan, dan izin tulis
    val folderCount = active.items.count { it.isDir }
    val fileCount = active.items.size - folderCount
    // Di flashdisk atau kartu SD, yang ditampilkan pemakaian penyimpanan itu, bukan penyimpanan internal
    val storage = remember(active.path, active.items) {
        storageBytes(Volumes.find(active.path)?.takeIf { !it.needsRoot }?.path ?: FileRepository.STORAGE_ROOT)
    }
    // Daftar flashdisk diperiksa ulang setiap menu samping dibuka
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) vm.refreshVolumes()
    }
    val writable = remember(active.path, state.rootMode) { File(active.path).canWrite() }
    val topInfo = stringResource(
        R.string.top_info,
        folderCount,
        fileCount,
        formatSize((storage.first - storage.second).coerceAtLeast(0L)),
        formatSize(storage.first),
        stringResource(if (writable) R.string.top_rw else R.string.top_ro),
    )

    // Menjalankan satu cara buka dari dialog "Buka dengan" untuk berkas [item]
    fun runOpenMode(item: FileItem, mode: OpenMode) {
        when (mode) {
            OpenMode.TEXT, OpenMode.XML, OpenMode.SVG ->
                onOpenEditor(EditorTarget(item.path, hex = false, root = state.rootMode))
            OpenMode.MARKDOWN -> onOpenPreview(PreviewTarget(item.path, markdown = true))
            OpenMode.HEX ->
                onOpenEditor(EditorTarget(item.path, hex = true, root = state.rootMode))
            OpenMode.APK_INFO -> vm.openApk(item.path)
            // Penampil arsip: isi arsip dibuka di panel seperti folder
            OpenMode.ARCHIVE ->
                if (item.extension.lowercase() in Archive.BROWSABLE) {
                    vm.openFolder(state.active, item.path)
                } else {
                    vm.notify(notReady)
                }
            OpenMode.SCRIPT -> onOpenTerminal(
                TerminalTarget(
                    cwd = FileRepository.parentOf(item.path),
                    command = "sh " + RootShell.quote(item.path),
                    root = state.rootMode,
                ),
            )
            OpenMode.IMAGE -> openFileAs(context, item, "image/*")
            OpenMode.MUSIC -> openFileAs(context, item, "audio/*")
            OpenMode.VIDEO -> openFileAs(context, item, "video/*")
            OpenMode.PDF -> openFileAs(context, item, "application/pdf")
            OpenMode.FONT -> onOpenTool(ToolKind.FONT, item.path)
            OpenMode.DEX -> onOpenTool(ToolKind.CLASSES, item.path)
            // Importir tanda tangan: berkas kunci disalin ke folder kunci Filemate
            OpenMode.SIGNATURE -> vm.importKey(item.path)
            // Fungsi Jar: mengubah JAR menjadi DEX di folder kerja Filemate
            OpenMode.JAR -> {
                vm.notify(jarWorking)
                scope.launch {
                    val out = File(FileRepository.FILEMATE_DIR, item.name.substringBeforeLast('.') + "-dex")
                    val minSdk = listPrefs.getInt(AppPrefs.KEY_JAR_MIN_SDK, Jar2Dex.DEFAULT_MIN_SDK)
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            Jar2Dex.convert(File(item.path), out, minSdk)
                            true
                        } catch (e: Throwable) {
                            false
                        }
                    }
                    vm.notify(if (ok) jarDone.format(out.path) else jarFailed)
                    vm.reloadAll()
                }
            }
            else -> vm.notify(notReady)
        }
    }

    // Membuka satu berkas (bukan folder) dengan cara yang cocok untuk jenisnya
    fun openFileItem(index: Int, item: FileItem) {
        vm.setActive(index)
        when {
            item.isApk -> vm.openApk(item.path)
            // ZIP yang dibuka dari dalam arsip lain juga ditampilkan seperti folder
            item.extension == "ZIP" -> vm.openFolder(index, item.path)
            // Berkas font langsung masuk penampil font, seperti MT Manager
            item.extension in FONT_EXT -> onOpenTool(ToolKind.FONT, item.path)
            // Berkas MD dan HTML dibuka sebagai pratinjau jika pengaturannya dinyalakan
            item.extension == "MD" && mdPreview -> onOpenPreview(PreviewTarget(item.path, markdown = true))
            (item.extension == "HTML" || item.extension == "HTM") && htmlPreview ->
                onOpenPreview(PreviewTarget(item.path, markdown = false))
            // Berkas teks langsung dibuka di editor. Jenis lain memunculkan pilihan "Buka dengan".
            item.extension in TEXT_EXT -> onOpenEditor(EditorTarget(item.path, hex = false, root = state.rootMode))
            else -> {
                // Cara buka bawaan yang pernah dipilih user (tekan lama di "Buka dengan") dipakai langsung
                val preset = OpenDefaults.get(context, item.extension)
                if (preset != null) runOpenMode(item, preset) else openWithTarget = item
            }
        }
    }

    // Berkas di dalam arsip dikeluarkan dulu ke folder sementara, lalu dibuka seperti berkas biasa
    // [chooser] true berarti dialog "Buka dengan" yang ditampilkan, bukan cara buka bawaan
    fun openArchiveEntry(index: Int, item: FileItem, chooser: Boolean = false) {
        scope.launch {
            val temp = withContext(Dispatchers.IO) {
                Archive.locate(item.path)?.let { Archive.extractToTemp(it, context.cacheDir) }
            }
            if (temp == null || !temp.isFile) {
                vm.notify(openFailed)
            } else {
                val copy = FileItem(
                    name = temp.name,
                    path = temp.path,
                    isDir = false,
                    isLink = false,
                    size = temp.length(),
                    modified = temp.lastModified(),
                )
                if (chooser) openWithTarget = copy else openFileItem(index, copy)
            }
        }
    }

    fun tapItem(index: Int, item: FileItem) {
        val panel = state.panels[index]
        when {
            panel.selected.isNotEmpty() -> vm.toggleSelect(index, item.path)
            item.isDir || (item.isLink && !item.isApk) -> vm.openFolder(index, item.path)
            Archive.inside(item.path) -> openArchiveEntry(index, item)
            // ZIP dibuka seperti folder, seperti MT Manager
            item.extension == "ZIP" -> vm.openFolder(index, item.path)
            else -> openFileItem(index, item)
        }
    }

    // Menutup menu tekan lama, lalu menjalankan aksinya
    fun closeMenu(action: () -> Unit) {
        menuTarget = null
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Menu samping hanya dibuka lewat tombol garis tiga. Geseran di daftar dipakai untuk menandai
        // berkas. Setelah terbuka, menu tetap bisa ditutup dengan geseran.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerSheet(
                bookmarks = state.bookmarks,
                localPaths = localPaths,
                toolOrder = drawerToolOrder(toolOrderSaved),
                toolHidden = toolHiddenSaved,
                onRemoveLocal = { path ->
                    val updated = localPaths - path
                    AppPrefs.saveLocalStorages(context, updated)
                    localPaths = updated
                },
                // Dialog dari menu samping dibuka setelah menu samping ditutup
                onDialog = { dialog ->
                    scope.launch { drawerState.close() }
                    drawerDialog = dialog
                },
                onGoto = { path ->
                    scope.launch { drawerState.close() }
                    vm.openFolder(state.active, path)
                },
                onRootDir = {
                    scope.launch { drawerState.close() }
                    vm.openRootDir()
                },
                onRemoveBookmark = { vm.removeBookmark(it) },
                onSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                onApps = {
                    scope.launch { drawerState.close() }
                    onOpenApps()
                },
                onTerminal = {
                    scope.launch { drawerState.close() }
                    onOpenTerminal(TerminalTarget(active.path, command = null, root = state.rootMode))
                },
                onTrash = {
                    scope.launch { drawerState.close() }
                    onOpenTrash()
                },
                onMcp = {
                    scope.launch { drawerState.close() }
                    mcpOpen = true
                },
                onTool = { kind ->
                    scope.launch { drawerState.close() }
                    when (kind) {
                        ToolKind.FTP, ToolKind.HTTP, ToolKind.LAN -> netDialog = kind
                        ToolKind.CLASSES -> {
                            val candidates = active.items.filter { it.isApk || it.extension == "DEX" }
                            when (candidates.size) {
                                0 -> vm.notify(noApkHere)
                                1 -> onOpenTool(ToolKind.CLASSES, candidates[0].path)
                                else -> classPickOpen = true
                            }
                        }
                        else -> onOpenTool(kind, null)
                    }
                },
                onNewText = {
                    scope.launch { drawerState.close() }
                    // Jika editor sedang diperkecil, alat ini membukanya lagi. Kalau tidak, buat berkas baru.
                    val minimized = EditorSession.current
                    if (minimized != null) onOpenEditor(minimized.target) else newTextOpen = true
                },
                onNotReady = {
                    scope.launch { drawerState.close() }
                    vm.notify(notReady)
                },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Body)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    path = active.path,
                    info = topInfo,
                    onJump = { jumpOpen = true },
                    dual = state.dual,
                    onToggleDual = { vm.setDual(!state.dual) },
                    showHidden = state.showHidden,
                    onMenu = { scope.launch { drawerState.open() } },
                    onSearch = { vm.toggleSearch(true) },
                    onSelectAll = { vm.selectAll(state.active) },
                    onSort = { sortOpen = true },
                    onTerminal = {
                        onOpenTerminal(TerminalTarget(active.path, command = null, root = state.rootMode))
                    },
                    onHome = { vm.setHome(active.path) },
                    onFilter = { vm.showDialog(AppDialog.TypeFilterPicker) },
                    onToggleHidden = { vm.toggleHidden() },
                    onBookmark = { vm.addBookmark(active.path) },
                    onSettings = onOpenSettings,
                    onExit = { (context as? Activity)?.finish() },
                    onRefresh = { vm.reloadAll() },
                    canSwap = state.dual,
                    onSwap = { vm.swapPanels() },
                )

                // Garis tipis di bawah bilah atas: bagian yang gelap menandai panel yang sedang aktif
                // Hanya tampil saat dua panel aktif
                if (state.dual) {
                    Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (state.active == 0) Muted else Body),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (state.active == 1) Muted else Body),
                        )
                    }
                }

                if (state.searchOpen) {
                    SearchField(
                        value = state.search,
                        onValueChange = { vm.setSearch(it) },
                        onClose = { vm.toggleSearch(false) },
                    )
                }

                if (active.selected.isNotEmpty()) {
                    SelectionBar(
                        count = active.selected.size,
                        showRename = singleItem,
                        showApk = singleApk,
                        onClose = { vm.clearSelection() },
                        onCopy = { vm.setClipFromSelection(move = false) },
                        onMove = { vm.setClipFromSelection(move = true) },
                        showOther = state.dual,
                        onCopyOther = { vm.transferSelectionToOther(move = false) },
                        onMoveOther = { vm.transferSelectionToOther(move = true) },
                        onRename = {
                            selectedItems.firstOrNull()?.let { vm.showDialog(AppDialog.Rename(it.path)) }
                        },
                        onDelete = { vm.deleteSelected() },
                        onApk = {
                            selectedItems.firstOrNull()?.let { vm.openApk(it.path) }
                        },
                    )
                }

                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val shown = if (state.dual) listOf(0, 1) else listOf(state.active)
                    shown.forEachIndexed { k, index ->
                        if (k > 0) VerticalDivider(color = LineColor)
                        val panel = state.panels[index]
                        val parent = vm.parentFolder(panel.path)
                        FilePanel(
                            panel = panel,
                            visible = visibleItems(panel.items, state.search, state.showHidden, state.typeFilter),
                            parentPath = if (state.search.isBlank()) parent else null,
                            onActivate = { vm.setActive(index) },
                            onOpenParent = { parent?.let { vm.openFolder(index, it) } },
                            onTap = { item -> tapItem(index, item) },
                            onIconTap = { item -> vm.toggleSelect(index, item.path) },
                            onLongPress = { item -> menuTarget = index to item },
                            fresh = state.fresh,
                            style = listStyle,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                PasteBar(
                    count = state.clip?.paths?.size,
                    onPaste = { vm.pasteHere() },
                )

                if (active.selected.isNotEmpty()) {
                    // Saat ada berkas ditandai, bilah bawah berganti jadi tombol pilihan, seperti MT Manager
                    SelectBottomBar(
                        extraBottom = bottomMargin,
                        onSelectAll = { vm.selectAll(state.active) },
                        onInvert = { vm.invertSelection(state.active) },
                        onCancel = { vm.clearSelection() },
                        onRange = { if (!vm.selectRange(state.active)) vm.notify(rangeHint) },
                        onSameType = { vm.selectSameType(state.active) },
                    )
                } else {
                    BottomBar(
                        canBack = active.back.isNotEmpty(),
                        canForward = active.forward.isNotEmpty(),
                        canSync = state.dual,
                        canUp = vm.parentFolder(active.path) != null,
                        extraBottom = bottomMargin,
                        onBack = { vm.historyBack(state.active) },
                        onForward = { vm.historyForward(state.active) },
                        onAdd = { vm.showDialog(AppDialog.NewFolder(active.path)) },
                        onSync = { vm.syncOtherPanel() },
                        onUp = { vm.goUp() },
                    )
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp),
            )
        }
    }

    // Menu kotak di tengah layar saat satu berkas ditekan lama
    menuTarget?.let { (index, item) ->
        // Jika berkas yang ditekan termasuk yang sedang dipilih, Salin, Pindah, Hapus, dan Kompres
        // berlaku untuk semua yang dipilih, bukan hanya berkas yang ditekan.
        val panel = state.panels[index]
        val picked = panel.items.filter { it.path in panel.selected }.map { it.path }
        val targets = if (item.path in panel.selected && picked.size > 1) picked else listOf(item.path)
        val targetItems = panel.items.filter { it.path in targets }.ifEmpty { listOf(item) }
        ItemMenu(
            item = item,
            multi = targets.size > 1,
            // Di dalam arsip hanya bisa menyalin keluar
            inArchive = Archive.inside(item.path),
            order = menuOrder,
            // Panel kiri menyalin ke kanan, panel kanan menyalin ke kiri
            towardRight = index == 0,
            dual = state.dual,
            onDismiss = { menuTarget = null },
            // Dua panel: langsung ke panel sebelah. Satu panel: disimpan dulu, lalu ditempel di folder tujuan.
            onCopyOther = {
                closeMenu {
                    if (state.dual) {
                        vm.transferToOtherPanel(index, targets, move = false)
                    } else {
                        vm.setActive(index)
                        vm.setClipPaths(targets, move = false)
                    }
                }
            },
            onMoveOther = {
                closeMenu {
                    if (state.dual) {
                        vm.transferToOtherPanel(index, targets, move = true)
                    } else {
                        vm.setActive(index)
                        vm.setClipPaths(targets, move = true)
                    }
                }
            },
            onRename = {
                closeMenu {
                    vm.setActive(index)
                    vm.showDialog(AppDialog.Rename(item.path))
                }
            },
            onDelete = {
                closeMenu {
                    vm.setActive(index)
                    vm.deletePaths(targets)
                }
            },
            onCompress = { closeMenu { vm.setActive(index); vm.compressPaths(targets) } },
            onAttributes = { closeMenu { vm.showDialog(AppDialog.Attributes(item)) } },
            onSend = { closeMenu { vm.notify(notReady) } },
            onShare = { closeMenu { shareFile(context, item) } },
            onOpenWith = {
                closeMenu {
                    if (Archive.inside(item.path)) openArchiveEntry(index, item, chooser = true) else openWithTarget = item
                }
            },
            onEditHex = { closeMenu { onOpenEditor(EditorTarget(item.path, hex = true, root = state.rootMode)) } },
            // "Alat": untuk APK membuka alat APK. Jenis lain belum punya alat.
            onTools = {
                closeMenu {
                    vm.setActive(index)
                    toolTargets = targetItems
                }
            },
            onEditText = { closeMenu { onOpenEditor(EditorTarget(item.path, hex = false, root = state.rootMode)) } },
            onBookmark = {
                closeMenu { vm.addBookmark(if (item.isDir) item.path else FileRepository.parentOf(item.path)) }
            },
            onExtract = { closeMenu { vm.setActive(index); vm.showDialog(AppDialog.Extract(item.path)) } },
            // Tanda tangani membuka dialog APK lalu langsung menandatanganinya
            onSign = { closeMenu { vm.openApk(item.path); vm.signApk() } },
            onRunScript = {
                closeMenu {
                    onOpenTerminal(
                        TerminalTarget(
                            cwd = FileRepository.parentOf(item.path),
                            command = "sh " + RootShell.quote(item.path),
                            root = state.rootMode,
                        ),
                    )
                }
            },
        )
    }

    // Dialog "Buka dengan": kotak ikon seperti MT Manager
    openWithTarget?.let { item ->
        fun done(action: () -> Unit) {
            openWithTarget = null
            action()
        }
        OpenWithDialog(
            extension = item.extension,
            onDismiss = { openWithTarget = null },
            onSystem = { done { openWith(context, item) } },
            onSystemAs = { mime -> done { openFileAs(context, item, mime) } },
            onPick = { mode -> done { runOpenMode(item, mode) } },
            // Tekan lama: dijadikan cara buka bawaan untuk akhiran berkas ini, lalu langsung dipakai
            onMakeDefault = { mode ->
                done {
                    if (item.extension.isNotEmpty()) {
                        OpenDefaults.set(context, item.extension, mode)
                        vm.notify(defaultSaved.format("." + item.extension.lowercase()))
                    }
                    runOpenMode(item, mode)
                }
            },
        )
    }

    if (mcpOpen) {
        McpDialog(onDismiss = { mcpOpen = false })
    }

    if (jumpOpen) {
        InputDialog(
            title = stringResource(R.string.dialog_jump),
            label = stringResource(R.string.dialog_jump_hint),
            initial = active.path,
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                jumpOpen = false
                val target = typed.trim().let { if (it.length > 1) it.trimEnd('/') else it }
                if (target.startsWith("/")) vm.openFolder(state.active, target) else vm.notify(invalidPath)
            },
            onDismiss = { jumpOpen = false },
        )
    }

    // Dialog dari menu samping: Manajemen Jarak Jauh, Manajer Plugin, Kelola grup alat, dan lainnya
    val toolLabels = DRAWER_TOOL_LABELS.map { it.first to stringResource(it.second) }.toMap()
    when (drawerDialog) {
        null -> Unit
        DrawerDialog.REMOTE -> RemoteHubDialog(
            onPick = { kind ->
                drawerDialog = null
                netDialog = kind
            },
            onDismiss = { drawerDialog = null },
        )
        DrawerDialog.PLUGINS -> PluginManagerDialog(onDismiss = { drawerDialog = null })
        DrawerDialog.TOOL_GROUP -> ToolGroupDialog(
            items = drawerToolOrder(toolOrderSaved).map { key -> key to (toolLabels[key] ?: key) },
            hidden = toolHiddenSaved,
            onSave = { order, hidden ->
                AppPrefs.saveToolLayout(context, order, hidden)
                toolOrderSaved = order.joinToString(",")
                toolHiddenSaved = hidden
                drawerDialog = null
            },
            onReset = {
                AppPrefs.saveToolLayout(context, emptyList(), emptySet())
                toolOrderSaved = ""
                toolHiddenSaved = emptySet()
                drawerDialog = null
            },
            onDismiss = { drawerDialog = null },
        )
        DrawerDialog.ADD_LOCAL -> InputDialog(
            title = stringResource(R.string.menu_add_local_storage),
            label = stringResource(R.string.local_add_hint),
            initial = "",
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                drawerDialog = null
                val folder = File(typed)
                if (folder.isDirectory && folder.canRead()) {
                    val updated = (localPaths + typed).distinct()
                    AppPrefs.saveLocalStorages(context, updated)
                    localPaths = updated
                } else {
                    vm.notify(invalidLocal)
                }
            },
            onDismiss = { drawerDialog = null },
        )
        DrawerDialog.FOLLOW_INFO -> FollowSystemDialog(
            onConfirm = {
                ThemeMode.followSystem = false
                ThemeMode.manualDark = !ThemeMode.dark
                AppPrefs.saveTheme(context)
                drawerDialog = null
            },
            onDismiss = { drawerDialog = null },
        )
    }

    netDialog?.let { kind ->
        NetServiceDialog(kind = kind, onDismiss = { netDialog = null })
    }

    // Alat "Editor teks": membuat berkas teks baru di folder yang sedang dibuka, lalu langsung membukanya
    if (newTextOpen) {
        InputDialog(
            title = stringResource(R.string.tool_text_editor),
            label = stringResource(R.string.new_text_hint),
            initial = "",
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                newTextOpen = false
                val name = typed.trim()
                val target = FileRepository.join(active.path, name)
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        name.isNotEmpty() && !name.contains('/') &&
                            (File(target).exists() || FileRepository.createFile(active.path, name, state.rootMode))
                    }
                    if (ok) {
                        vm.reloadAll()
                        onOpenEditor(EditorTarget(target, hex = false, root = state.rootMode))
                    } else {
                        vm.notify(notReadyName)
                    }
                }
            },
            onDismiss = { newTextOpen = false },
        )
    }

    if (classPickOpen) {
        val candidates = active.items.filter { it.isApk || it.extension == "DEX" }
        AlertDialog(
            onDismissRequest = { classPickOpen = false },
            containerColor = DialogBg,
            title = { Text(stringResource(R.string.tool_class_analysis), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    candidates.forEach { item ->
                        Text(
                            text = item.name,
                            color = Ink,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    classPickOpen = false
                                    onOpenTool(ToolKind.CLASSES, item.path)
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { classPickOpen = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Accent)
                }
            },
        )
    }

    installAsk?.let { warnings ->
        AlertDialog(
            onDismissRequest = { installAsk = null },
            containerColor = DialogBg,
            title = { Text(stringResource(R.string.apk_btn_install), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    warnings.forEach { warning ->
                        Text(text = warning, color = Danger, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(stringResource(R.string.install_confirm_body), color = Ink, fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    installAsk = null
                    doInstall()
                }) {
                    Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { installAsk = null }) {
                    Text(stringResource(R.string.btn_cancel), color = Accent)
                }
            },
        )
    }

    // Dialog "Peralatan" dari menu tekan lama
    toolTargets?.let { targets ->
        FileToolsDialog(
            targets = targets,
            onDismiss = { toolTargets = null },
            onPick = { tool ->
                toolTargets = null
                val first = targets.first()
                when (tool) {
                    FileTool.APK -> vm.openApk(first.path)
                    FileTool.SPLIT, FileTool.ENCRYPT, FileTool.DECRYPT -> pendingTool = tool to first
                    FileTool.JOIN -> vm.joinParts(first.path)
                    FileTool.SWAP -> vm.swapNames(targets[0].path, targets[1].path)
                    FileTool.SYMLINK -> vm.makeSymlink(first.path)
                    FileTool.SHORTCUT -> vm.notify(if (pinShortcut(context, first)) shortcutAsked else shortcutFailed)
                    FileTool.EXPORT -> vm.exportFileList(targets.map { it.path })
                }
            },
        )
    }

    // Isian lanjutan untuk alat yang butuh ukuran potongan atau kata sandi
    pendingTool?.let { (tool, item) ->
        if (tool == FileTool.SPLIT) {
            InputDialog(
                title = stringResource(tool.label),
                label = stringResource(R.string.ftool_split_hint),
                initial = "10",
                confirmText = stringResource(R.string.btn_ok),
                onConfirm = { typed ->
                    pendingTool = null
                    vm.splitFile(item.path, typed.toIntOrNull()?.coerceIn(1, 4096) ?: 10)
                },
                onDismiss = { pendingTool = null },
            )
        } else {
            // Kata sandi umum yang tersimpan bisa dipilih langsung di dialog ini
            PasswordDialog(
                title = stringResource(tool.label),
                onConfirm = { typed ->
                    pendingTool = null
                    if (tool == FileTool.ENCRYPT) vm.encryptFile(item.path, typed) else vm.decryptFile(item.path, typed)
                },
                onDismiss = { pendingTool = null },
            )
        }
    }

    if (sortOpen) {
        SortDialog(
            current = state.sort,
            onPick = {
                vm.setSort(it)
                sortOpen = false
            },
            onDismiss = { sortOpen = false },
        )
    }

    // Dialog yang sedang aktif
    when (val dialog = state.dialog) {
        is AppDialog.NewFolder -> NewItemDialog(
            parentDir = dialog.parentDir,
            onCreateFile = { name -> vm.requestAction(FileAction.NewFile(dialog.parentDir, name)) },
            onCreateFolder = { name -> vm.requestAction(FileAction.NewFolder(dialog.parentDir, name)) },
            onDismiss = { vm.dismissDialog() },
        )

        is AppDialog.Rename -> InputDialog(
            title = stringResource(R.string.dialog_rename),
            label = stringResource(R.string.label_new_name),
            initial = FileRepository.nameOf(dialog.path),
            confirmText = stringResource(R.string.btn_save),
            onConfirm = { name -> vm.requestAction(FileAction.Rename(dialog.path, name)) },
            onDismiss = { vm.dismissDialog() },
        )

        is AppDialog.Confirm -> {
            val action = dialog.action
            val deletePaths = (action as? FileAction.Delete)?.paths.orEmpty()
            // Jika yang dihapus hanya satu, nama dan ukurannya ditampilkan di dialog
            val single = if (deletePaths.size == 1) {
                state.panels.flatMap { it.items }.firstOrNull { it.path == deletePaths[0] }
            } else {
                null
            }
            ConfirmDialog(
                action = action,
                system = dialog.system,
                single = single,
                trashAvailable = trashOn && deletePaths.isNotEmpty() && deletePaths.all { Trash.supports(it) },
                trashDefault = trashDefault,
                showWarning = deleteWarning,
                onConfirm = { vm.confirmAction(it) },
                onDismiss = { vm.dismissDialog() },
            )
        }

        is AppDialog.Apk -> state.apk?.let { info ->
            ApkDialog(
                info = info,
                install = state.apkInstall,
                onSign = { vm.signApk() },
                onUnpack = { vm.unpackApk() },
                onRepack = { vm.repackApk() },
                // Pengaturan "Verifikasi pemasangan APK" dan "Konfirmasi ganda": periksa dulu, tanyakan jika perlu
                onInstall = {
                    val verify = listPrefs.getBoolean(AppPrefs.KEY_VERIFY_INSTALL, true)
                    val confirm = listPrefs.getBoolean(AppPrefs.KEY_INSTALL_CONFIRM, false)
                    scope.launch {
                        val warnings = if (verify) withContext(Dispatchers.IO) { vm.installWarnings() } else emptyList()
                        if (warnings.isEmpty() && !confirm) doInstall() else installAsk = warnings
                    }
                },
                onBrowse = {
                    vm.dismissApk()
                    vm.openFolder(state.active, info.path)
                },
                onHideInstall = { vm.hideApkInstall() },
                onCloseInstall = { vm.closeApkInstall() },
                onDetails = {
                    vm.closeApkInstall()
                    openAppDetails(context, info.packageName)
                },
                onLaunch = {
                    vm.closeApkInstall()
                    launchApp(context, info.packageName)
                },
                onDismiss = { vm.dismissApk() },
            )
        }

        is AppDialog.Extract -> ExtractDialog(
            path = dialog.path,
            otherPanelDir = if (state.dual) state.panels[1 - state.active].path else null,
            onExtract = { target, deleteSource -> vm.extractZip(dialog.path, target, deleteSource) },
            onDismiss = { vm.dismissDialog() },
        )

        is AppDialog.Attributes -> AttributesDialog(
            item = dialog.item,
            root = state.rootMode,
            onChanged = { vm.reloadAll() },
            onDismiss = { vm.dismissDialog() },
        )

        AppDialog.TypeFilterPicker -> TypeFilterDialog(
            current = state.typeFilter,
            onPick = {
                vm.setTypeFilter(it)
                vm.dismissDialog()
            },
            onDismiss = { vm.dismissDialog() },
        )

        null -> Unit
    }
}

/** Bilah atas biru: tombol menu, jalur folder dengan baris keterangan kecil, dan menu titik tiga. */
@Composable
private fun TopBar(
    path: String,
    info: String,
    onJump: () -> Unit,
    dual: Boolean,
    onToggleDual: () -> Unit,
    showHidden: Boolean,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onSelectAll: () -> Unit,
    onSort: () -> Unit,
    onTerminal: () -> Unit,
    onHome: () -> Unit,
    onFilter: () -> Unit,
    onToggleHidden: () -> Unit,
    onBookmark: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    onRefresh: () -> Unit,
    canSwap: Boolean,
    onSwap: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBrush)
            .statusBarsPadding()
            .height(60.dp)
            .padding(start = 4.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.cd_menu),
                tint = Color.White,
            )
        }

        // Mengetuk jalur membuka dialog "Lompat" untuk mengetik alamat folder, seperti NP Manager
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onJump)
                .padding(start = 10.dp),
        ) {
            Text(
                text = path,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = info,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Tombol kecil dua panel: putih penuh saat dua panel aktif, bergaris saat satu panel
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (dual) Color.White else Color.Transparent)
                .border(1.dp, Color.White, RoundedCornerShape(7.dp))
                .clickable(onClick = onToggleDual),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ViewColumn,
                contentDescription = stringResource(R.string.btn_dual),
                tint = if (dual) Primary else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }

        // Menu titik tiga, urutannya mengikuti NP Manager
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.cd_more),
                    tint = Color.White,
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = DialogBg,
            ) {
                // Urutan mengikuti menu titik tiga MT Manager
                OverflowItem(stringResource(R.string.bar_refresh), Icons.Filled.Refresh) {
                    menuOpen = false
                    onRefresh()
                }
                OverflowItem(stringResource(R.string.menu_search), Icons.Filled.Search) {
                    menuOpen = false
                    onSearch()
                }
                OverflowItem(stringResource(R.string.menu_select_all), Icons.Filled.SelectAll) {
                    menuOpen = false
                    onSelectAll()
                }
                OverflowItem(stringResource(R.string.menu_filter), Icons.Filled.FilterList) {
                    menuOpen = false
                    onFilter()
                }
                OverflowItem(stringResource(R.string.menu_sort), Icons.Filled.Sort) {
                    menuOpen = false
                    onSort()
                }
                OverflowItem(stringResource(R.string.menu_terminal), Icons.Filled.Terminal) {
                    menuOpen = false
                    onTerminal()
                }
                // Dicentang berarti file tersembunyi tidak ditampilkan
                OverflowItem(
                    label = stringResource(R.string.menu_hide_files),
                    icon = Icons.Filled.Visibility,
                    checked = !showHidden,
                ) {
                    menuOpen = false
                    onToggleHidden()
                }
                OverflowItem(stringResource(R.string.menu_bookmark), Icons.Filled.Bookmark) {
                    menuOpen = false
                    onBookmark()
                }
                OverflowItem(stringResource(R.string.menu_set_home), Icons.Filled.Home) {
                    menuOpen = false
                    onHome()
                }
                if (canSwap) {
                    OverflowItem(stringResource(R.string.menu_swap_panels), Icons.Filled.SwapHoriz) {
                        menuOpen = false
                        onSwap()
                    }
                }
                OverflowItem(stringResource(R.string.drawer_settings), Icons.Filled.Settings) {
                    menuOpen = false
                    onSettings()
                }
                OverflowItem(stringResource(R.string.menu_exit), Icons.Filled.ExitToApp) {
                    menuOpen = false
                    onExit()
                }
            }
        }
    }
}

/** Satu baris menu titik tiga. Jika [checked] diisi, kotak centang tampil di kanan. */
@Composable
private fun OverflowItem(
    label: String,
    icon: ImageVector,
    checked: Boolean? = null,
    onClick: () -> Unit,
) {
    val trailing: (@Composable () -> Unit)? = if (checked == null) {
        null
    } else {
        {
            Icon(
                imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (checked) Ink else Muted,
            )
        }
    }
    DropdownMenuItem(
        text = { Text(text = label, color = Ink, fontSize = 15.sp) },
        onClick = onClick,
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = MenuIcon) },
        trailingIcon = trailing,
    )
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_close),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SelectionBar(
    count: Int,
    showRename: Boolean,
    showApk: Boolean,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    showOther: Boolean,
    onCopyOther: () -> Unit,
    onMoveOther: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onApk: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SelectTint)
            .padding(start = 2.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_close),
                tint = Ink,
            )
        }
        Text(
            text = stringResource(R.string.selected_count, count),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(end = 4.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showApk) {
                ActionText(stringResource(R.string.btn_apk_tools), Accent, onApk)
            }
            ActionText(stringResource(R.string.btn_copy), Accent, onCopy)
            ActionText(stringResource(R.string.btn_move), Accent, onMove)
            if (showOther) {
                ActionText(stringResource(R.string.menu_copy_other), Accent, onCopyOther)
                ActionText(stringResource(R.string.menu_move_other), Accent, onMoveOther)
            }
            if (showRename) {
                ActionText(stringResource(R.string.btn_rename), Accent, onRename)
            }
            ActionText(stringResource(R.string.btn_delete), Danger, onDelete)
        }
    }
}

@Composable
private fun ActionText(label: String, color: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp),
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Tombol "Tempel" di atas bilah bawah. Hanya tampil jika ada berkas yang disalin atau dipotong. */
@Composable
private fun PasteBar(count: Int?, onPaste: () -> Unit) {
    if (count == null) return
    Button(
        onClick = onPaste,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary),
    ) {
        Text(stringResource(R.string.btn_paste, count), fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Bilah bawah berisi lima ikon abu-abu tanpa tulisan, susunannya seperti MT Manager:
 * kembali, maju, tambah, samakan panel sebelah, dan naik satu folder.
 */
@Composable
private fun BottomBar(
    canBack: Boolean,
    canForward: Boolean,
    canSync: Boolean,
    canUp: Boolean,
    extraBottom: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onAdd: () -> Unit,
    onSync: () -> Unit,
    onUp: () -> Unit,
) {
    BarFrame(extraBottom) {
        BarButton(Icons.Filled.ArrowBack, stringResource(R.string.bar_back), canBack, onBack)
        BarButton(Icons.Filled.ArrowForward, stringResource(R.string.bar_forward), canForward, onForward)
        BarButton(Icons.Filled.Add, stringResource(R.string.bar_add), true, onAdd)
        BarButton(Icons.Filled.SyncAlt, stringResource(R.string.bar_sync), canSync, onSync)
        BarButton(Icons.Filled.ArrowUpward, stringResource(R.string.bar_up), canUp, onUp)
    }
}

/** Bilah bawah saat ada berkas ditandai: pilih semua, balik pilihan, batal, pilih rentang, pilih sejenis. */
@Composable
private fun SelectBottomBar(
    extraBottom: Int,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onCancel: () -> Unit,
    onRange: () -> Unit,
    onSameType: () -> Unit,
) {
    BarFrame(extraBottom) {
        BarButton(Icons.Filled.SelectAll, stringResource(R.string.menu_select_all), true, onSelectAll)
        BarButton(Icons.Filled.FlipToBack, stringResource(R.string.bar_invert), true, onInvert)
        BarButton(Icons.Filled.Close, stringResource(R.string.btn_cancel), true, onCancel)
        BarButton(Icons.Filled.UnfoldMore, stringResource(R.string.bar_range), true, onRange)
        BarButton(Icons.Filled.Category, stringResource(R.string.bar_same_type), true, onSameType)
    }
}

/** Bingkai bilah bawah: garis tipis di atas, lima tombol rata, dan jarak tambahan dari pengaturan. */
@Composable
private fun BarFrame(extraBottom: Int, buttons: @Composable RowScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Body)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = LineColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            content = buttons,
        )
        // Jarak tambahan dari pengaturan "Ukuran margin bawah bilah alat bawah"
        if (extraBottom > 0) Spacer(Modifier.height(extraBottom.dp))
    }
}

@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Muted else Disabled,
        )
    }
}

/** Satu pilihan di menu kotak. */
private class MenuEntry(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
)

/**
 * Menu saat berkas ditekan lama: kotak putih di tengah layar, pilihan disusun dua kolom
 * dengan ikon di kiri. Isi dan urutannya mengikuti NP Manager.
 */
@Composable
private fun ItemMenu(
    item: FileItem,
    multi: Boolean,
    inArchive: Boolean,
    order: List<String>,
    towardRight: Boolean,
    dual: Boolean,
    onDismiss: () -> Unit,
    onCopyOther: () -> Unit,
    onMoveOther: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onAttributes: () -> Unit,
    onSend: () -> Unit,
    onShare: () -> Unit,
    onOpenWith: () -> Unit,
    onEditHex: () -> Unit,
    onTools: () -> Unit,
    onEditText: () -> Unit,
    onBookmark: () -> Unit,
    onExtract: () -> Unit,
    onSign: () -> Unit,
    onRunScript: () -> Unit,
) {
    val isFile = !item.isDir
    val isZip = isFile && item.extension == "ZIP"
    val isApk = isFile && item.isApk
    val copyText = stringResource(R.string.btn_copy)
    val moveText = stringResource(R.string.btn_move)
    // Tanda panah menunjukkan arah panel tujuan
    val copyLabel = if (!dual) copyText else if (towardRight) "$copyText ->" else "<- $copyText"
    val moveLabel = if (!dual) moveText else if (towardRight) "$moveText ->" else "<- $moveText"

    // Di dalam arsip: hanya menyalin keluar. Beberapa berkas dipilih: hanya pilihan yang berlaku untuk semuanya.
    val entries = if (inArchive) listOfNotNull(
        MenuEntry(copyLabel, Icons.Filled.ContentCopy, Ink, onCopyOther),
        if (isFile && !multi) MenuEntry(stringResource(R.string.menu_open_with), Icons.Filled.OpenInNew, Ink, onOpenWith) else null,
    ) else if (multi) listOf(
        MenuEntry(copyLabel, Icons.Filled.ContentCopy, Ink, onCopyOther),
        MenuEntry(moveLabel, Icons.Filled.ContentCut, Ink, onMoveOther),
        MenuEntry(stringResource(R.string.btn_delete), Icons.Filled.Delete, Danger, onDelete),
        MenuEntry(stringResource(R.string.menu_tools), Icons.Filled.Build, Ink, onTools),
        MenuEntry(stringResource(R.string.menu_compress), Icons.Filled.Archive, Ink, onCompress),
    ) else {
        // Setiap pilihan punya kunci, supaya urutannya bisa diatur di Pengaturan ("Urutkan menu berkas")
        val all: Map<String, MenuEntry?> = mapOf(
            "copy" to MenuEntry(copyLabel, Icons.Filled.ContentCopy, Ink, onCopyOther),
            "move" to MenuEntry(moveLabel, Icons.Filled.ContentCut, Ink, onMoveOther),
            "rename" to MenuEntry(stringResource(R.string.btn_rename), Icons.Filled.Edit, Ink, onRename),
            "delete" to MenuEntry(stringResource(R.string.btn_delete), Icons.Filled.Delete, Danger, onDelete),
            "compress" to MenuEntry(stringResource(R.string.menu_compress), Icons.Filled.Archive, Ink, onCompress),
            "attributes" to MenuEntry(stringResource(R.string.menu_attributes), Icons.Filled.Info, Ink, onAttributes),
            "send" to if (isFile) MenuEntry(stringResource(R.string.menu_send_device), Icons.Filled.SendToMobile, Ink, onSend) else null,
            "share" to if (isFile) MenuEntry(stringResource(R.string.menu_share), Icons.Filled.Share, Ink, onShare) else null,
            "openwith" to if (isFile) MenuEntry(stringResource(R.string.menu_open_with), Icons.Filled.OpenInNew, Ink, onOpenWith) else null,
            "hex" to if (isFile) MenuEntry(stringResource(R.string.menu_edit_hex), Icons.Filled.Code, Ink, onEditHex) else null,
            "tools" to MenuEntry(stringResource(R.string.menu_tools), Icons.Filled.Build, Ink, onTools),
            "edittext" to if (isFile) MenuEntry(stringResource(R.string.menu_edit_text), Icons.Filled.Description, Ink, onEditText) else null,
            "bookmark" to MenuEntry(stringResource(R.string.menu_bookmark), Icons.Filled.Bookmark, Ink, onBookmark),
            // APK hanya bisa ditandatangani. ZIP dan APK bisa diekstrak, berkas lain menjalankan skrip
            "sign" to if (isApk) MenuEntry(stringResource(R.string.menu_sign), Icons.Filled.VpnKey, Ink, onSign) else null,
            "extra" to if (isZip || isApk) {
                MenuEntry(stringResource(R.string.menu_extract), Icons.Filled.Unarchive, Ink, onExtract)
            } else if (isFile) {
                MenuEntry(stringResource(R.string.menu_run_script), Icons.Filled.Terminal, Ink, onRunScript)
            } else {
                null
            },
        )
        order.mapNotNull { all[it] }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DialogBg,
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                entries.chunked(2).forEach { pair ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { entry ->
                            MenuCell(entry = entry, modifier = Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCell(entry: MenuEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = if (entry.color == Ink) MenuIcon else entry.color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = entry.label,
            color = entry.color,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Atribut berkas atau folder: label di kiri, isi di kanan, seperti NP Manager. */
@Composable
private fun AttributesDialog(
    item: FileItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.dialog_attributes_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                AttrRow(stringResource(R.string.attr_name), item.name)
                AttrRow(stringResource(R.string.attr_location), FileRepository.parentOf(item.path))
                AttrRow(
                    label = stringResource(R.string.attr_type),
                    value = stringResource(if (item.isDir) R.string.attr_type_folder else R.string.attr_type_file),
                )
                // Ukuran folder tidak dihitung, karena bisa lama jika isinya banyak
                AttrRow(
                    label = stringResource(R.string.attr_size),
                    value = if (item.isDir) "-" else "${formatSize(item.size)} (${item.size})",
                )
                AttrRow(stringResource(R.string.attr_modified), formatDateFull(item.modified))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun AttrRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 7.dp)) {
        Text(
            text = label,
            color = Muted,
            fontSize = 14.sp,
            modifier = Modifier.width(92.dp),
        )
        Text(text = value, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

/** Pilihan urutan daftar berkas, dibuka dari menu titik tiga. */
@Composable
private fun SortDialog(
    current: SortMode,
    onPick: (SortMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.menu_sort), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                SortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = { onPick(mode) })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(sortLabelRes(mode)), fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Accent)
            }
        },
    )
}

private fun sortLabelRes(mode: SortMode): Int = when (mode) {
    SortMode.NAME -> R.string.sort_name
    SortMode.SIZE -> R.string.sort_size
    SortMode.DATE -> R.string.sort_date
}

@Composable
private fun TypeFilterDialog(
    current: TypeFilter,
    onPick: (TypeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.dialog_filter_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                TypeFilter.entries.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(filter) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = filter == current, onClick = { onPick(filter) })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(filterLabelRes(filter)), fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Accent)
            }
        },
    )
}

private fun filterLabelRes(filter: TypeFilter): Int = when (filter) {
    TypeFilter.ALL -> R.string.filter_all
    TypeFilter.FOLDER -> R.string.filter_folder
    TypeFilter.APK -> R.string.filter_apk
    TypeFilter.MEDIA -> R.string.filter_media
    TypeFilter.DOCUMENT -> R.string.filter_document
}

/** Satu alat di menu samping. [action] null berarti fiturnya belum dibuat. */
private class DrawerTool(val key: String, val label: Int, val icon: ImageVector, val action: (() -> Unit)?)

/**
 * Menu samping seperti MT Manager: kepala berwarna tema dengan tombol tema dan menu tambahan,
 * lalu bagian Lokal, Bookmark, Peralatan, dan Kontak yang masing-masing bisa dilipat.
 */
@Composable
private fun DrawerSheet(
    bookmarks: List<String>,
    localPaths: List<String>,
    toolOrder: List<String>,
    toolHidden: Set<String>,
    onGoto: (String) -> Unit,
    onRootDir: () -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onRemoveLocal: (String) -> Unit,
    onDialog: (DrawerDialog) -> Unit,
    onSettings: () -> Unit,
    onApps: () -> Unit,
    onTerminal: () -> Unit,
    onTrash: () -> Unit,
    onMcp: () -> Unit,
    onTool: (ToolKind) -> Unit,
    onNewText: () -> Unit,
    onNotReady: () -> Unit,
) {
    val storageLabel = stringResource(R.string.title_storage)
    val context = LocalContext.current
    val volumes by Volumes.list.collectAsState()
    val version = remember { appVersionName(context) }
    var localOpen by remember { mutableStateOf(true) }
    var bookmarkOpen by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(true) }
    var otherOpen by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    // Dibaca di sini supaya ikon tema dan centang ikut berubah saat mode diganti
    val darkNow = ThemeMode.dark
    val followNow = ThemeMode.followSystem

    // Ikon matahari atau bulan. Jika "Tema mengikuti sistem" aktif, user ditanya dulu.
    fun toggleDark() {
        if (ThemeMode.followSystem) {
            onDialog(DrawerDialog.FOLLOW_INFO)
        } else {
            ThemeMode.manualDark = !ThemeMode.manualDark
            AppPrefs.saveTheme(context)
        }
    }

    // Centang "Tema mengikuti sistem". Saat dimatikan, mode yang sedang tampil tetap dipakai.
    fun toggleFollow() {
        if (ThemeMode.followSystem) {
            ThemeMode.manualDark = ThemeMode.dark
            ThemeMode.followSystem = false
        } else {
            ThemeMode.followSystem = true
        }
        AppPrefs.saveTheme(context)
    }

    // Alat MT Manager dulu (Tempat sampah sampai Instruksi Smali), lalu alat khusus Filemate.
    // Yang belum dibuat menampilkan pesan saat diketuk.
    val allTools: Map<String, DrawerTool> = listOf(
        DrawerTool("trash", R.string.drawer_trash, Icons.Filled.Delete, onTrash),
        DrawerTool("plugins", R.string.drawer_plugins, Icons.Filled.Extension, { onDialog(DrawerDialog.PLUGINS) }),
        DrawerTool("remote", R.string.drawer_remote, Icons.Filled.Dns, { onDialog(DrawerDialog.REMOTE) }),
        DrawerTool("color", R.string.drawer_color, Icons.Filled.Colorize, { onTool(ToolKind.COLOR) }),
        DrawerTool("keys", R.string.drawer_keys, Icons.Filled.VpnKey, { onTool(ToolKind.KEYS) }),
        DrawerTool("passwords", R.string.drawer_passwords, Icons.Filled.Lock, { onTool(ToolKind.PASSWORDS) }),
        DrawerTool("extract_apk", R.string.drawer_extract_apk, Icons.Filled.Archive, onApps),
        DrawerTool("text", R.string.drawer_text, Icons.Filled.EditNote, onNewText),
        DrawerTool("terminal", R.string.drawer_terminal, Icons.Filled.Terminal, onTerminal),
        DrawerTool("activity", R.string.drawer_activity, Icons.Filled.TrackChanges, { onTool(ToolKind.ACTIVITY) }),
        DrawerTool("smali", R.string.drawer_smali, Icons.Filled.Description, { onTool(ToolKind.SMALI) }),
        DrawerTool("apps", R.string.tool_apps, Icons.Filled.Layers, onApps),
        DrawerTool("toolbox", R.string.tool_toolbox, Icons.Filled.Build, { onTool(ToolKind.TOOLBOX) }),
        DrawerTool("mcp", R.string.tool_apk_mcp, Icons.Filled.SmartToy, onMcp),
        DrawerTool("ftp", R.string.tool_ftp, Icons.Filled.Cloud, { onTool(ToolKind.FTP) }),
        DrawerTool("http", R.string.tool_http, Icons.Filled.Language, { onTool(ToolKind.HTTP) }),
        DrawerTool("lan", R.string.tool_lan, Icons.Filled.Wifi, { onTool(ToolKind.LAN) }),
        DrawerTool("java2smali", R.string.tool_java2smali, Icons.Filled.Coffee, null),
        DrawerTool("network", R.string.tool_network, Icons.Filled.Public, { onTool(ToolKind.NETWORK) }),
        DrawerTool("layout", R.string.tool_layout_viewer, Icons.Filled.ViewQuilt, { onTool(ToolKind.LAYOUT) }),
        DrawerTool("ruler", R.string.tool_ruler, Icons.Filled.Straighten, { onTool(ToolKind.RULER) }),
        DrawerTool("decibel", R.string.tool_decibel, Icons.Filled.GraphicEq, { onTool(ToolKind.DECIBEL) }),
        DrawerTool("reflection", R.string.tool_reflection, Icons.Filled.Code, null),
        DrawerTool("classes", R.string.tool_class_analysis, Icons.Filled.Handyman, { onTool(ToolKind.CLASSES) }),
        DrawerTool("timestamp", R.string.tool_timestamp, Icons.Filled.Schedule, { onTool(ToolKind.TIMESTAMP) }),
        DrawerTool("prefs", R.string.drawer_settings, Icons.Filled.Settings, onSettings),
    ).associateBy { it.key }
    // Alat yang tampil: urutan dari Kelola grup alat, tanpa yang disembunyikan
    val tools = toolOrder.mapNotNull { allTools[it] }.filter { it.key !in toolHidden }

    ModalDrawerSheet(
        drawerShape = RectangleShape,
        drawerContainerColor = Body,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.width(300.dp),
    ) {
        // Kepala menu: logo, nama aplikasi dan versi, tombol tema, dan menu tambahan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .statusBarsPadding()
                .padding(start = 18.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_filemate),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.drawer_version, version),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
            IconButton(onClick = { toggleDark() }) {
                Icon(
                    imageVector = if (darkNow) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.drawer_theme_toggle),
                    tint = Color.White,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.cd_more),
                        tint = Color.White,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_follow_system), color = Ink) },
                        leadingIcon = {
                            Checkbox(
                                checked = followNow,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = Accent),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            toggleFollow()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_net_storage), color = Ink) },
                        onClick = {
                            menuOpen = false
                            onNotReady()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_local_storage), color = Ink) },
                        onClick = {
                            menuOpen = false
                            onDialog(DrawerDialog.ADD_LOCAL)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_net_group), color = Ink) },
                        onClick = {
                            menuOpen = false
                            onNotReady()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_manage_tools), color = Ink) },
                        onClick = {
                            menuOpen = false
                            onDialog(DrawerDialog.TOOL_GROUP)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.drawer_settings), color = Ink) },
                        onClick = {
                            menuOpen = false
                            onSettings()
                        },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            DrawerSection(stringResource(R.string.drawer_section_local), localOpen) { localOpen = !localOpen }
            if (localOpen) {
                // Akses root aktif otomatis. Jika belum, mengetuk Direktori Akar mencobanya lagi.
                StorageRow(
                    title = stringResource(R.string.drawer_root_dir),
                    icon = Icons.Filled.PhoneAndroid,
                    path = "/",
                    onClick = onRootDir,
                )
                StorageRow(
                    title = stringResource(R.string.drawer_storage_dir),
                    icon = Icons.Filled.SdStorage,
                    path = FileRepository.STORAGE_ROOT,
                    onClick = { onGoto(FileRepository.STORAGE_ROOT) },
                )
                // Flashdisk OTG dan kartu SD yang sedang terpasang
                volumes.forEach { volume ->
                    StorageRow(
                        title = volume.label,
                        icon = Icons.Filled.Usb,
                        path = volume.path,
                        onClick = { onGoto(volume.path) },
                    )
                }
                // Penyimpanan lokal tambahan dari menu ⋮. Folder yang sudah tidak ada tidak ditampilkan.
                localPaths.filter { File(it).isDirectory }.forEach { path ->
                    StorageRow(
                        title = path.substringAfterLast('/').ifEmpty { path },
                        icon = Icons.Filled.Folder,
                        path = path,
                        onClick = { onGoto(path) },
                        onRemove = { onRemoveLocal(path) },
                    )
                }
            }

            DrawerSection(stringResource(R.string.drawer_bookmarks), bookmarkOpen) { bookmarkOpen = !bookmarkOpen }
            if (bookmarkOpen) {
                if (bookmarks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.drawer_bookmarks_empty),
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
                bookmarks.forEach { path ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGoto(path) }
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DrawerIcon(Icons.Filled.Bookmark)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folderTitle(path, storageLabel),
                                color = Ink,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = path,
                                color = Muted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onRemoveBookmark(path) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_remove_bookmark),
                                tint = Muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            DrawerSection(stringResource(R.string.drawer_section_tools), toolsOpen) { toolsOpen = !toolsOpen }
            if (toolsOpen) {
                tools.forEach { tool ->
                    DrawerRow(stringResource(tool.label), tool.icon, tool.action ?: onNotReady)
                }
            }

            DrawerSection(stringResource(R.string.drawer_section_contact), otherOpen) { otherOpen = !otherOpen }
            if (otherOpen) {
                ContactRow("Telegram", "@Skypin969", Icons.Filled.Send) {
                    openLink(context, "https://t.me/Skypin969")
                }
                ContactRow("GitHub", "github.com/skypin969", Icons.Filled.Person) {
                    openLink(context, "https://github.com/skypin969")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Membuka tautan di aplikasi yang cocok (Telegram, peramban). */
private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        Toast.makeText(context, R.string.contact_open_failed, Toast.LENGTH_SHORT).show()
    }
}

/** Baris kontak di menu samping: ikon, nama layanan, dan alamat akun di bawahnya. */
@Composable
private fun ContactRow(label: String, address: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawerIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = MenuIcon, fontSize = 15.sp, maxLines = 1)
            Text(
                text = address,
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Satu baris menu samping: ikon dalam kotak biru muda, lalu nama. */
@Composable
private fun DrawerRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawerIcon(icon)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = MenuIcon,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Judul bagian menu samping: garis biru kecil, nama bagian, dan panah lipat di kanan. */
@Composable
private fun DrawerSection(title: String, open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(Accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MenuIcon,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Ikon biru di dalam kotak biru muda, dipakai semua baris menu samping. */
@Composable
private fun DrawerIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(IconBox),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Baris penyimpanan: nama, batang pemakaian dengan persen, dan keterangan terpakai/tersedia.
 * [onRemove] dipakai untuk penyimpanan lokal tambahan dan menampilkan tombol hapus di kanan.
 */
@Composable
private fun StorageRow(
    title: String,
    icon: ImageVector,
    path: String,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    val (total, free) = remember(path) { storageBytes(path) }
    // Flashdisk yang hanya terbaca lewat root tidak bisa diukur dari sini, jadi cukup tampilkan jalurnya
    if (total <= 0L) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrawerIcon(icon)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = path, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            RemoveButton(onRemove)
        }
        return
    }
    val used = (total - free).coerceAtLeast(0L)
    val fraction = if (total > 0) used.toFloat() / total.toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawerIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Ink, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.weight(1f).height(3.dp),
                    color = Accent,
                    trackColor = LineColor,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "${(fraction * 100).toInt()}%", color = Accent, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.drawer_usage, formatSize(used), formatSize(free)),
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RemoveButton(onRemove)
    }
}

/** Tombol hapus kecil untuk penyimpanan lokal tambahan. Tidak tampil jika [onRemove] kosong. */
@Composable
private fun RemoveButton(onRemove: (() -> Unit)?) {
    if (onRemove == null) return
    IconButton(onClick = onRemove) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cd_remove_local),
            tint = Muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Kapasitas dan sisa ruang di [path], dalam byte. */
private fun storageBytes(path: String): Pair<Long, Long> = try {
    val stat = StatFs(path)
    Pair(stat.totalBytes, stat.availableBytes)
} catch (e: Exception) {
    Pair(0L, 0L)
}
