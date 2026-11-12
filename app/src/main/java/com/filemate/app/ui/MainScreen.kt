package com.filemate.app.ui

import android.app.Activity
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filemate.app.R
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import kotlinx.coroutines.launch

/** Layar utama: dua panel, bilah atas, bilah pilihan, dan bilah aksi di bawah. */
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    vm: FileManagerViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    // Berkas yang sedang ditekan lama, beserta indeks panelnya. Null jika menu tidak tampil.
    var menuTarget by remember { mutableStateOf<Pair<Int, FileItem>?>(null) }

    // Tombol kembali: tutup menu, lalu tutup pencarian/pilihan, lalu naik satu folder, lalu keluar
    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            !vm.goBack() -> (context as? Activity)?.finish()
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

    fun tapItem(index: Int, item: FileItem) {
        val panel = state.panels[index]
        when {
            panel.selected.isNotEmpty() -> vm.toggleSelect(index, item.path)
            item.isDir || (item.isLink && !item.isApk) -> vm.openFolder(index, item.path)
            item.isApk -> {
                vm.setActive(index)
                vm.openApk(item.path)
            }
            else -> {
                vm.setActive(index)
                openFile(context, item)
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
                rootMode = state.rootMode,
                bookmarks = state.bookmarks,
                onGoto = { path ->
                    scope.launch { drawerState.close() }
                    vm.openFolder(state.active, path)
                },
                onRootChange = { vm.setRootMode(it) },
                onRemoveBookmark = { vm.removeBookmark(it) },
                onSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    path = active.path,
                    dual = state.dual,
                    showHidden = state.showHidden,
                    sort = state.sort,
                    onMenu = { scope.launch { drawerState.open() } },
                    onToggleDual = { vm.setDual(!state.dual) },
                    onSearch = { vm.toggleSearch(true) },
                    onSelectAll = { vm.selectAll(state.active) },
                    onSort = { vm.setSort(it) },
                    onFilter = { vm.showDialog(AppDialog.TypeFilterPicker) },
                    onToggleHidden = { vm.toggleHidden() },
                    onBookmark = { vm.addBookmark(active.path) },
                    onSettings = onOpenSettings,
                    onExit = { (context as? Activity)?.finish() },
                )

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
                            isActive = state.active == index,
                            parentPath = if (state.search.isBlank()) parent else null,
                            onActivate = { vm.setActive(index) },
                            onOpenParent = { parent?.let { vm.openFolder(index, it) } },
                            onTap = { item -> tapItem(index, item) },
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
                    .padding(bottom = 130.dp),
            )
        }
    }

    // Menu tekan lama pada satu berkas
    menuTarget?.let { (index, item) ->
        ItemSheet(
            item = item,
            onDismiss = { menuTarget = null },
            onSelect = { closeMenu { vm.toggleSelect(index, item.path) } },
            onCopy = {
                closeMenu {
                    vm.setActive(index)
                    vm.setClipPaths(listOf(item.path), move = false)
                }
            },
            onMove = {
                closeMenu {
                    vm.setActive(index)
                    vm.setClipPaths(listOf(item.path), move = true)
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
            onApk = { closeMenu { vm.openApk(item.path) } },
        )
    }

    // Dialog yang sedang aktif
    when (val dialog = state.dialog) {
        is AppDialog.NewFolder -> InputDialog(
            title = stringResource(R.string.dialog_new_folder),
            label = stringResource(R.string.label_folder_name),
            initial = "",
            confirmText = stringResource(R.string.btn_create),
            onConfirm = { name -> vm.requestAction(FileAction.NewFolder(dialog.parentDir, name)) },
            onDismiss = { vm.dismissDialog() },
        )

        is AppDialog.Rename -> InputDialog(
            title = stringResource(R.string.dialog_rename),
            label = stringResource(R.string.label_new_name),
            initial = FileRepository.nameOf(dialog.path),
            confirmText = stringResource(R.string.btn_rename),
            onConfirm = { name -> vm.requestAction(FileAction.Rename(dialog.path, name)) },
            onDismiss = { vm.dismissDialog() },
        )

        is AppDialog.Confirm -> ConfirmDialog(
            action = dialog.action,
            system = dialog.system,
            onConfirm = { vm.confirmAction(dialog.action) },
            onDismiss = { vm.dismissDialog() },
        )

        is AppDialog.Apk -> state.apk?.let { info ->
            ApkDialog(
                info = info,
                onUnpack = { vm.unpackApk() },
                onRepack = { vm.repackApk() },
                onSign = { vm.signApk() },
                onDismiss = { vm.dismissApk() },
            )
        }

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

@Composable
private fun TopBar(
    path: String,
    dual: Boolean,
    showHidden: Boolean,
    sort: SortMode,
    onMenu: () -> Unit,
    onToggleDual: () -> Unit,
    onSearch: () -> Unit,
    onSelectAll: () -> Unit,
    onSort: (SortMode) -> Unit,
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
            .background(HeaderGradient)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.cd_menu),
                tint = Color.White,
            )
        }

        // Lokasi folder yang sedang dibuka
        Text(
            text = path,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
        )

        // Tombol "Dua panel" berupa ikon kecil: putih saat aktif, garis tipis saat tidak aktif
        Surface(
            onClick = onToggleDual,
            shape = RoundedCornerShape(8.dp),
            color = if (dual) Color.White else Color.Transparent,
            border = if (dual) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(end = 4.dp)
                .size(36.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ViewColumn,
                    contentDescription = stringResource(R.string.btn_dual),
                    tint = if (dual) Blue700 else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Menu titik tiga: cari, urutkan, filter, bookmark, pengaturan, keluar
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
            ) {
                OverflowItem(stringResource(R.string.menu_search), Icons.Filled.Search) {
                    menuOpen = false
                    onSearch()
                }
                OverflowItem(stringResource(R.string.menu_select_all), Icons.Filled.SelectAll) {
                    menuOpen = false
                    onSelectAll()
                }
                HorizontalDivider()
                OverflowItem(
                    label = stringResource(R.string.menu_sort_name),
                    icon = Icons.Filled.Sort,
                    checked = sort == SortMode.NAME,
                ) {
                    menuOpen = false
                    onSort(SortMode.NAME)
                }
                OverflowItem(
                    label = stringResource(R.string.menu_sort_size),
                    icon = Icons.Filled.Sort,
                    checked = sort == SortMode.SIZE,
                ) {
                    menuOpen = false
                    onSort(SortMode.SIZE)
                }
                OverflowItem(
                    label = stringResource(R.string.menu_sort_date),
                    icon = Icons.Filled.Sort,
                    checked = sort == SortMode.DATE,
                ) {
                    menuOpen = false
                    onSort(SortMode.DATE)
                }
                HorizontalDivider()
                OverflowItem(stringResource(R.string.menu_filter), Icons.Filled.FilterList) {
                    menuOpen = false
                    onFilter()
                }
                OverflowItem(
                    label = stringResource(R.string.menu_show_hidden),
                    icon = Icons.Filled.Visibility,
                    checked = showHidden,
                ) {
                    menuOpen = false
                    onToggleHidden()
                }
                HorizontalDivider()
                OverflowItem(stringResource(R.string.menu_bookmark), Icons.Filled.Bookmark) {
                    menuOpen = false
                    onBookmark()
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

@Composable
private fun OverflowItem(
    label: String,
    icon: ImageVector,
    checked: Boolean = false,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
        trailingIcon = {
            if (checked) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Blue700)
            }
        },
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
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onApk: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blue100)
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
                ActionText(stringResource(R.string.btn_apk_tools), Blue700, onApk)
            }
            ActionText(stringResource(R.string.btn_copy), Blue700, onCopy)
            ActionText(stringResource(R.string.btn_move), Blue700, onMove)
            if (showRename) {
                ActionText(stringResource(R.string.btn_rename), Blue700, onRename)
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
        colors = ButtonDefaults.buttonColors(containerColor = Blue700),
    ) {
        Text(stringResource(R.string.btn_paste, count), fontWeight = FontWeight.SemiBold)
    }
}

/** Bilah bawah: kembali, maju, tambah folder, tukar panel, muat ulang. */
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
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = LineColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
    val tint = if (enabled) Blue700 else Muted.copy(alpha = 0.4f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
        }
        Text(
            text = label,
            color = if (enabled) Ink else tint,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

/** Lembar bawah saat berkas ditekan lama. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemSheet(
    item: FileItem,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onApk: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = item.name,
            color = Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SheetAction(stringResource(R.string.menu_select), Icons.Filled.Check, onSelect)
        SheetAction(stringResource(R.string.btn_copy), Icons.Filled.ContentCopy, onCopy)
        SheetAction(stringResource(R.string.btn_move), Icons.Filled.DriveFileMove, onMove)
        SheetAction(stringResource(R.string.btn_rename), Icons.Filled.Edit, onRename)
        SheetAction(stringResource(R.string.btn_delete), Icons.Filled.Delete, onDelete, color = Danger)
        if (item.isApk) {
            SheetAction(stringResource(R.string.btn_apk_tools), Icons.Filled.Build, onApk)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SheetAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = Ink,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(16.dp))
        Text(text = label, color = color, fontSize = 15.sp)
    }
}

@Composable
private fun TypeFilterDialog(
    current: TypeFilter,
    onPick: (TypeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_filter_title)) },
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
                Text(stringResource(R.string.btn_close))
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

@Composable
private fun DrawerSheet(
    rootMode: Boolean,
    bookmarks: List<String>,
    onGoto: (String) -> Unit,
    onRootChange: (Boolean) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val storageLabel = stringResource(R.string.title_storage)

    ModalDrawerSheet {
        Text(
            text = stringResource(R.string.app_name),
            color = Blue700,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 12.dp),
        )

        StorageInfo()

        DrawerItem(stringResource(R.string.drawer_storage), Icons.Filled.Storage) {
            onGoto(FileRepository.STORAGE_ROOT)
        }
        DrawerItem(stringResource(R.string.drawer_download), Icons.Filled.Download) {
            onGoto(FileRepository.STORAGE_ROOT + "/Download")
        }
        DrawerItem(stringResource(R.string.drawer_documents), Icons.Filled.Description) {
            onGoto(FileRepository.STORAGE_ROOT + "/Documents")
        }
        DrawerItem(stringResource(R.string.drawer_dcim), Icons.Filled.CameraAlt) {
            onGoto(FileRepository.STORAGE_ROOT + "/DCIM")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LineColor)

        Text(
            text = stringResource(R.string.drawer_bookmarks),
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp),
        )
        if (bookmarks.isEmpty()) {
            Text(
                text = stringResource(R.string.drawer_bookmarks_empty),
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 28.dp, end = 16.dp, bottom = 8.dp),
            )
        }
        bookmarks.forEach { path ->
            NavigationDrawerItem(
                label = {
                    Column {
                        Text(
                            text = folderTitle(path, storageLabel),
                            fontSize = 14.sp,
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
                },
                selected = false,
                onClick = { onGoto(path) },
                icon = { Icon(imageVector = Icons.Filled.Bookmark, contentDescription = null) },
                badge = {
                    IconButton(onClick = { onRemoveBookmark(path) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_remove_bookmark),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LineColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = Blue700,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.drawer_root),
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.drawer_root_desc),
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            Switch(checked = rootMode, onCheckedChange = onRootChange)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LineColor)

        DrawerItem(stringResource(R.string.drawer_settings), Icons.Filled.Settings, onSettings)
    }
}

/** Batang kapasitas penyimpanan utama, dengan teks ringkas di atasnya. */
@Composable
private fun StorageInfo() {
    val (total, free) = remember { storageBytes() }
    val used = (total - free).coerceAtLeast(0L)
    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.drawer_storage_used, formatSize(used), formatSize(total)),
            color = Ink,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) used.toFloat() / total.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = Blue700,
            trackColor = Blue100,
        )
    }
}

/** Kapasitas dan sisa ruang penyimpanan utama, dalam byte. */
private fun storageBytes(): Pair<Long, Long> = try {
    val stat = StatFs(FileRepository.STORAGE_ROOT)
    Pair(stat.totalBytes, stat.availableBytes)
} catch (e: Exception) {
    Pair(0L, 0L)
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
