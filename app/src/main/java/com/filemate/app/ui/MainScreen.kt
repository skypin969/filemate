package com.filemate.app.ui

import android.app.Activity
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
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import com.filemate.app.data.RootShell
import com.filemate.app.data.Trash
import java.io.File
import kotlinx.coroutines.launch

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
    var mcpOpen by remember { mutableStateOf(false) }

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
    val storage = remember(active.path, active.items) { storageBytes(FileRepository.STORAGE_ROOT) }
    val writable = remember(active.path, state.rootMode) { File(active.path).canWrite() }
    val topInfo = stringResource(
        R.string.top_info,
        folderCount,
        fileCount,
        formatSize((storage.first - storage.second).coerceAtLeast(0L)),
        formatSize(storage.first),
        stringResource(if (writable) R.string.top_rw else R.string.top_ro),
    )

    fun tapItem(index: Int, item: FileItem) {
        val panel = state.panels[index]
        when {
            panel.selected.isNotEmpty() -> vm.toggleSelect(index, item.path)
            item.isDir || (item.isLink && !item.isApk) -> vm.openFolder(index, item.path)
            item.isApk -> {
                vm.setActive(index)
                vm.openApk(item.path)
            }
            // Berkas teks langsung dibuka di editor. Jenis lain memunculkan pilihan "Buka dengan", seperti NP Manager.
            item.extension in TEXT_EXT -> {
                vm.setActive(index)
                onOpenEditor(EditorTarget(item.path, hex = false, root = state.rootMode))
            }
            else -> {
                vm.setActive(index)
                openWithTarget = item
            }
        }
    }

    // Menutup menu tekan lama, lalu menjalankan aksinya
    fun closeMenu(action: () -> Unit) {
        menuTarget = null
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerSheet(
                bookmarks = state.bookmarks,
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
                onNotReady = {
                    scope.launch { drawerState.close() }
                    vm.notify(notReady)
                },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    path = active.path,
                    info = topInfo,
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
                )

                // Garis tipis di bawah bilah atas: bagian yang gelap menandai panel yang sedang aktif
                // Hanya tampil saat dua panel aktif
                if (state.dual) {
                    Row(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (state.active == 0) Muted else Color.White),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (state.active == 1) Muted else Color.White),
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
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                PasteBar(
                    count = state.clip?.paths?.size,
                    onPaste = { vm.pasteHere() },
                )

                BottomBar(
                    canBack = active.back.isNotEmpty(),
                    canForward = active.forward.isNotEmpty(),
                    canSwap = state.dual,
                    onBack = { vm.historyBack(state.active) },
                    onForward = { vm.historyForward(state.active) },
                    onAdd = { vm.showDialog(AppDialog.NewFolder(active.path)) },
                    onSwap = { vm.swapPanels() },
                    onRefresh = { vm.reloadAll() },
                )
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
        ItemMenu(
            item = item,
            // Panel kiri menyalin ke kanan, panel kanan menyalin ke kiri
            towardRight = index == 0,
            dual = state.dual,
            onDismiss = { menuTarget = null },
            // Dua panel: langsung ke panel sebelah. Satu panel: disimpan dulu, lalu ditempel di folder tujuan.
            onCopyOther = {
                closeMenu {
                    if (state.dual) {
                        vm.transferToOtherPanel(index, listOf(item.path), move = false)
                    } else {
                        vm.setActive(index)
                        vm.setClipPaths(listOf(item.path), move = false)
                    }
                }
            },
            onMoveOther = {
                closeMenu {
                    if (state.dual) {
                        vm.transferToOtherPanel(index, listOf(item.path), move = true)
                    } else {
                        vm.setActive(index)
                        vm.setClipPaths(listOf(item.path), move = true)
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
                    vm.deletePaths(listOf(item.path))
                }
            },
            onCompress = { closeMenu { vm.setActive(index); vm.compressPath(item.path) } },
            onAttributes = { closeMenu { vm.showDialog(AppDialog.Attributes(item)) } },
            onSend = { closeMenu { vm.notify(notReady) } },
            onShare = { closeMenu { shareFile(context, item) } },
            onOpenWith = { closeMenu { openWithTarget = item } },
            onEditHex = { closeMenu { onOpenEditor(EditorTarget(item.path, hex = true, root = state.rootMode)) } },
            // "Alat": untuk APK membuka alat APK. Jenis lain belum punya alat.
            onTools = {
                closeMenu {
                    vm.setActive(index)
                    if (item.isApk) vm.openApk(item.path) else vm.notify(notReady)
                }
            },
            onEditText = { closeMenu { onOpenEditor(EditorTarget(item.path, hex = false, root = state.rootMode)) } },
            onBookmark = {
                closeMenu { vm.addBookmark(if (item.isDir) item.path else FileRepository.parentOf(item.path)) }
            },
            onExtract = { closeMenu { vm.setActive(index); vm.extractZip(item.path) } },
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

    // Dialog "Buka dengan": kotak ikon seperti NP Manager
    openWithTarget?.let { item ->
        fun done(action: () -> Unit) {
            openWithTarget = null
            action()
        }
        OpenWithDialog(
            onDismiss = { openWithTarget = null },
            onSystem = { done { openWith(context, item) } },
            onPick = { mode ->
                done {
                    when (mode) {
                        OpenMode.TEXT, OpenMode.XML, OpenMode.SVG, OpenMode.MARKDOWN ->
                            onOpenEditor(EditorTarget(item.path, hex = false, root = state.rootMode))
                        OpenMode.HEX ->
                            onOpenEditor(EditorTarget(item.path, hex = true, root = state.rootMode))
                        OpenMode.APK_INFO -> vm.openApk(item.path)
                        OpenMode.ARCHIVE ->
                            if (item.extension == "ZIP") vm.extractZip(item.path) else vm.notify(notReady)
                        OpenMode.SCRIPT -> onOpenTerminal(
                            TerminalTarget(
                                cwd = FileRepository.parentOf(item.path),
                                command = "sh " + RootShell.quote(item.path),
                                root = state.rootMode,
                            ),
                        )
                        OpenMode.IMAGE -> openFileAs(context, item, "image/*")
                        OpenMode.MUSIC -> openFileAs(context, item, "audio/*")
                        OpenMode.PDF -> openFileAs(context, item, "application/pdf")
                        OpenMode.FONT -> openFileAs(context, item, "font/*")
                        else -> vm.notify(notReady)
                    }
                }
            },
        )
    }

    if (mcpOpen) {
        McpDialog(onDismiss = { mcpOpen = false })
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
                trashAvailable = deletePaths.isNotEmpty() && deletePaths.all { Trash.supports(it) },
                onConfirm = { vm.confirmAction(it) },
                onDismiss = { vm.dismissDialog() },
            )
        }

        is AppDialog.Apk -> state.apk?.let { info ->
            ApkDialog(
                info = info,
                onUnpack = { vm.unpackApk() },
                onRepack = { vm.repackApk() },
                onSign = { vm.signApk() },
                onDismiss = { vm.dismissApk() },
            )
        }

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

        Column(
            modifier = Modifier
                .weight(1f)
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
                containerColor = Color.White,
            ) {
                OverflowItem(stringResource(R.string.menu_search), Icons.Filled.Search) {
                    menuOpen = false
                    onSearch()
                }
                OverflowItem(stringResource(R.string.menu_select_all), Icons.Filled.SelectAll) {
                    menuOpen = false
                    onSelectAll()
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
                OverflowItem(stringResource(R.string.menu_filter), Icons.Filled.FilterList) {
                    menuOpen = false
                    onFilter()
                }
                OverflowItem(stringResource(R.string.menu_bookmark), Icons.Filled.Bookmark) {
                    menuOpen = false
                    onBookmark()
                }
                OverflowItem(stringResource(R.string.menu_set_home), Icons.Filled.Home) {
                    menuOpen = false
                    onHome()
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

/** Bilah bawah berisi lima ikon abu-abu tanpa tulisan: kembali, maju, tambah folder, tukar panel, muat ulang. */
@Composable
private fun BottomBar(
    canBack: Boolean,
    canForward: Boolean,
    canSwap: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onAdd: () -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = LineColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(Icons.Filled.ArrowBack, stringResource(R.string.bar_back), canBack, onBack)
            BarButton(Icons.Filled.ArrowForward, stringResource(R.string.bar_forward), canForward, onForward)
            BarButton(Icons.Filled.Add, stringResource(R.string.bar_add), true, onAdd)
            BarButton(Icons.Filled.SwapHoriz, stringResource(R.string.bar_swap), canSwap, onSwap)
            BarButton(Icons.Filled.Refresh, stringResource(R.string.bar_refresh), true, onRefresh)
        }
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
    onRunScript: () -> Unit,
) {
    val isFile = !item.isDir
    val isZip = isFile && item.extension == "ZIP"
    val copyText = stringResource(R.string.btn_copy)
    val moveText = stringResource(R.string.btn_move)
    // Tanda panah menunjukkan arah panel tujuan
    val copyLabel = if (!dual) copyText else if (towardRight) "$copyText ->" else "<- $copyText"
    val moveLabel = if (!dual) moveText else if (towardRight) "$moveText ->" else "<- $moveText"

    val entries = listOfNotNull(
        MenuEntry(copyLabel, Icons.Filled.ContentCopy, Ink, onCopyOther),
        MenuEntry(moveLabel, Icons.Filled.ContentCut, Ink, onMoveOther),
        MenuEntry(stringResource(R.string.btn_rename), Icons.Filled.Edit, Ink, onRename),
        MenuEntry(stringResource(R.string.btn_delete), Icons.Filled.Delete, Danger, onDelete),
        MenuEntry(stringResource(R.string.menu_compress), Icons.Filled.Archive, Ink, onCompress),
        MenuEntry(stringResource(R.string.menu_attributes), Icons.Filled.Info, Ink, onAttributes),
        if (isFile) MenuEntry(stringResource(R.string.menu_send_device), Icons.Filled.SendToMobile, Ink, onSend) else null,
        if (isFile) MenuEntry(stringResource(R.string.menu_share), Icons.Filled.Share, Ink, onShare) else null,
        if (isFile) MenuEntry(stringResource(R.string.menu_open_with), Icons.Filled.OpenInNew, Ink, onOpenWith) else null,
        if (isFile) MenuEntry(stringResource(R.string.menu_edit_hex), Icons.Filled.Code, Ink, onEditHex) else null,
        if (isFile) MenuEntry(stringResource(R.string.menu_tools), Icons.Filled.Build, Ink, onTools) else null,
        if (isFile) MenuEntry(stringResource(R.string.menu_edit_text), Icons.Filled.Description, Ink, onEditText) else null,
        MenuEntry(stringResource(R.string.menu_bookmark), Icons.Filled.Bookmark, Ink, onBookmark),
        // Untuk ZIP, pilihan terakhir adalah mengekstrak, seperti NP Manager
        if (isZip) {
            MenuEntry(stringResource(R.string.menu_extract), Icons.Filled.Unarchive, Ink, onExtract)
        } else if (isFile) {
            MenuEntry(stringResource(R.string.menu_run_script), Icons.Filled.Terminal, Ink, onRunScript)
        } else {
            null
        },
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
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
        containerColor = Color.White,
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
        containerColor = Color.White,
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
        containerColor = Color.White,
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
private class DrawerTool(val label: Int, val icon: ImageVector, val action: (() -> Unit)?)

/**
 * Menu samping, susunannya mengikuti NP Manager: kepala biru, lalu bagian Lokal,
 * Bookmark, Alat, dan Lainnya yang masing-masing bisa dilipat.
 */
@Composable
private fun DrawerSheet(
    bookmarks: List<String>,
    onGoto: (String) -> Unit,
    onRootDir: () -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onSettings: () -> Unit,
    onApps: () -> Unit,
    onTerminal: () -> Unit,
    onTrash: () -> Unit,
    onMcp: () -> Unit,
    onNotReady: () -> Unit,
) {
    val storageLabel = stringResource(R.string.title_storage)
    var localOpen by remember { mutableStateOf(true) }
    var bookmarkOpen by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(true) }
    var otherOpen by remember { mutableStateOf(true) }

    // Urutan mengikuti NP Manager. Yang belum dibuat menampilkan pesan saat diketuk.
    val tools = listOf(
        DrawerTool(R.string.tool_apps, Icons.Filled.Layers, onApps),
        DrawerTool(R.string.tool_toolbox, Icons.Filled.Build, null),
        DrawerTool(R.string.tool_apk_mcp, Icons.Filled.SmartToy, onMcp),
        DrawerTool(R.string.tool_ftp, Icons.Filled.Cloud, null),
        DrawerTool(R.string.tool_http, Icons.Filled.Language, null),
        DrawerTool(R.string.tool_lan, Icons.Filled.Wifi, null),
        DrawerTool(R.string.tool_recycle, Icons.Filled.Delete, onTrash),
        DrawerTool(R.string.tool_java2smali, Icons.Filled.Coffee, null),
        DrawerTool(R.string.tool_smali_query, Icons.Filled.Description, null),
        DrawerTool(R.string.tool_network, Icons.Filled.Public, null),
        DrawerTool(R.string.tool_terminal, Icons.Filled.Terminal, onTerminal),
        DrawerTool(R.string.tool_activity_record, Icons.Filled.TrackChanges, null),
        DrawerTool(R.string.tool_layout_viewer, Icons.Filled.ViewQuilt, null),
        DrawerTool(R.string.tool_color_picker, Icons.Filled.Colorize, null),
        DrawerTool(R.string.tool_ruler, Icons.Filled.Straighten, null),
        DrawerTool(R.string.tool_decibel, Icons.Filled.GraphicEq, null),
        DrawerTool(R.string.tool_text_editor, Icons.Filled.EditNote, null),
        DrawerTool(R.string.tool_reflection, Icons.Filled.Code, null),
        DrawerTool(R.string.tool_class_analysis, Icons.Filled.Handyman, null),
        DrawerTool(R.string.tool_timestamp, Icons.Filled.Schedule, null),
    )

    ModalDrawerSheet(
        drawerShape = RectangleShape,
        drawerContainerColor = Color.White,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.width(300.dp),
    ) {
        // Kepala menu: logo dan nama aplikasi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Accent)
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
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
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            DrawerSection(stringResource(R.string.drawer_section_local), localOpen) { localOpen = !localOpen }
            if (localOpen) {
                // Akses root aktif otomatis. Jika belum, mengetuk Direktori root mencobanya lagi.
                StorageRow(
                    title = stringResource(R.string.drawer_root_dir),
                    icon = Icons.Filled.PhoneAndroid,
                    path = "/",
                    onClick = onRootDir,
                )
                StorageRow(
                    title = storageLabel,
                    icon = Icons.Filled.SdStorage,
                    path = FileRepository.STORAGE_ROOT,
                    onClick = { onGoto(FileRepository.STORAGE_ROOT) },
                )
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

            DrawerSection(stringResource(R.string.drawer_section_other), otherOpen) { otherOpen = !otherOpen }
            if (otherOpen) {
                DrawerRow(stringResource(R.string.drawer_settings), Icons.Filled.Settings, onSettings)
            }
            Spacer(Modifier.height(16.dp))
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

/** Baris penyimpanan: nama, batang pemakaian dengan persen, dan keterangan terpakai/tersedia. */
@Composable
private fun StorageRow(
    title: String,
    icon: ImageVector,
    path: String,
    onClick: () -> Unit,
) {
    val (total, free) = remember(path) { storageBytes(path) }
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
    }
}

/** Kapasitas dan sisa ruang di [path], dalam byte. */
private fun storageBytes(path: String): Pair<Long, Long> = try {
    val stat = StatFs(path)
    Pair(stat.totalBytes, stat.availableBytes)
} catch (e: Exception) {
    Pair(0L, 0L)
}
