package com.filemate.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.filemate.app.R
import com.filemate.app.data.Trash
import com.filemate.app.data.TrashEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Layar Recycle Bin seperti NP Manager: daftar berkas yang sudah dihapus.
 * Mengetuk atau menggeser satu berkas menandainya. Berkas yang ditandai bisa dipulihkan
 * atau dihapus permanen sekaligus lewat tombol di bawah.
 */
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<TrashEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    // Nama berkas simpanan yang sedang ditandai
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }

    suspend fun reload() {
        entries = withContext(Dispatchers.IO) { Trash.list() }
        selected = selected.filter { name -> entries.any { it.stored.name == name } }.toSet()
        loaded = true
    }

    fun toggle(entry: TrashEntry) {
        val name = entry.stored.name
        selected = if (name in selected) selected - name else selected + name
    }

    /** Menjalankan [work] untuk semua berkas yang ditandai, lalu memuat ulang daftar. */
    fun forSelected(work: (TrashEntry) -> Unit) {
        val targets = entries.filter { it.stored.name in selected }
        selected = emptySet()
        scope.launch {
            withContext(Dispatchers.IO) { targets.forEach(work) }
            reload()
        }
    }

    // Tombol kembali membatalkan tanda dulu, baru keluar
    BackHandler { if (selected.isNotEmpty()) selected = emptySet() else onBack() }
    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBrush)
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected.isEmpty()) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White,
                    )
                }
                Text(
                    text = stringResource(R.string.tool_recycle),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                IconButton(onClick = { selected = emptySet() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = Color.White,
                    )
                }
                Text(
                    text = stringResource(R.string.selected_count, selected.size),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                IconButton(onClick = { selected = entries.map { it.stored.name }.toSet() }) {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = stringResource(R.string.menu_select_all),
                        tint = Color.White,
                    )
                }
            }
        }

        if (loaded && entries.isEmpty()) {
            Text(
                text = stringResource(R.string.trash_empty),
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(entries, key = { it.stored.name }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (entry.stored.name in selected) SelectTint else Color.Transparent)
                        .swipeSelect(entry.stored.name) { toggle(entry) }
                        .clickable { toggle(entry) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (entry.stored.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = if (entry.stored.isDirectory) FolderColor else FileTint,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            color = Ink,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatDate(entry.stored.lastModified()) +
                                if (entry.stored.isDirectory) "" else "  " + formatSize(entry.stored.length()),
                            color = Muted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        // Tombol untuk berkas yang ditandai
        if (selected.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftGray)
                    .navigationBarsPadding(),
            ) {
                TrashChoice(stringResource(R.string.trash_restore), Ink, Modifier.weight(1f)) {
                    forSelected { Trash.restore(it) }
                }
                TrashChoice(stringResource(R.string.trash_delete_forever), Danger, Modifier.weight(1f)) {
                    confirmDelete = true
                }
            }
        } else {
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (confirmDelete) {
        Dialog(onDismissRequest = { confirmDelete = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) {
                    Text(
                        text = stringResource(R.string.trash_delete_forever),
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 22.dp),
                    )
                    Text(
                        text = stringResource(R.string.trash_confirm_forever, selected.size),
                        color = Danger,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TrashChoice(stringResource(R.string.btn_cancel), Ink, Modifier.weight(1f)) {
                            confirmDelete = false
                        }
                        TrashChoice(stringResource(R.string.btn_ok), Danger, Modifier.weight(1f)) {
                            confirmDelete = false
                            forSelected { Trash.deleteForever(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashChoice(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 15.dp),
    )
}
