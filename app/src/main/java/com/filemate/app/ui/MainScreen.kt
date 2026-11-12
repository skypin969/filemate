package com.filemate.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerSheet(
                rootMode = state.rootMode,
                onGoto = { path ->
                    scope.launch { drawerState.close() }
                    vm.openFolder(state.active, path)
                },
                onRootChange = { vm.setRootMode(it) },
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
                    onMenu = { scope.launch { drawerState.open() } },
                    onToggleDual = { vm.setDual(!state.dual) },
                    onSearch = { vm.toggleSearch(!state.searchOpen) },
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
                        FilePanel(
                            panel = state.panels[index],
                            isActive = state.active == index,
                            search = state.search,
                            onActivate = { vm.setActive(index) },
                            onTap = { item -> tapItem(index, item) },
                            onLongPress = { item -> vm.toggleSelect(index, item.path) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                BottomBar(
                    clipCount = state.clip?.paths?.size,
                    onNewFolder = { vm.showDialog(AppDialog.NewFolder(active.path)) },
                    onSort = { vm.setSort(it) },
                    onPaste = { vm.pasteHere() },
                )
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            )
        }
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

        null -> Unit
    }
}

@Composable
private fun TopBar(
    path: String,
    dual: Boolean,
    onMenu: () -> Unit,
    onToggleDual: () -> Unit,
    onSearch: () -> Unit,
) {
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

        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = Color.White,
            )
        }
    }
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

@Composable
private fun BottomBar(
    clipCount: Int?,
    onNewFolder: () -> Unit,
    onSort: (SortMode) -> Unit,
    onPaste: () -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onNewFolder,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Blue700),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue700),
        ) {
            Text(stringResource(R.string.btn_new_folder), fontWeight = FontWeight.SemiBold)
        }

        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { sortMenuOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Blue200),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
            ) {
                Text(stringResource(R.string.btn_sort), fontWeight = FontWeight.SemiBold)
            }
            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { sortMenuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_name)) },
                    onClick = {
                        onSort(SortMode.NAME)
                        sortMenuOpen = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_size)) },
                    onClick = {
                        onSort(SortMode.SIZE)
                        sortMenuOpen = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_date)) },
                    onClick = {
                        onSort(SortMode.DATE)
                        sortMenuOpen = false
                    },
                )
            }
        }

        if (clipCount != null) {
            Button(
                onClick = onPaste,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue700),
            ) {
                Text(stringResource(R.string.btn_paste, clipCount), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DrawerSheet(
    rootMode: Boolean,
    onGoto: (String) -> Unit,
    onRootChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = stringResource(R.string.app_name),
            color = Blue700,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 12.dp),
        )

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
