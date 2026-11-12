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
 * Mengetuk satu berkas memberi pilihan memulihkan atau menghapusnya selamanya.
 */
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<TrashEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<TrashEntry?>(null) }

    suspend fun reload() {
        entries = withContext(Dispatchers.IO) { Trash.list() }
        loaded = true
    }

    BackHandler { onBack() }
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
        }

        if (loaded && entries.isEmpty()) {
            Text(
                text = stringResource(R.string.trash_empty),
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
            items(entries, key = { it.stored.name }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { picked = entry }
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
    }

    val entry = picked
    if (entry != null) {
        Dialog(onDismissRequest = { picked = null }) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(modifier = Modifier.padding(vertical = 14.dp)) {
                    Text(
                        text = entry.name,
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                    )
                    TrashChoice(stringResource(R.string.trash_restore)) {
                        picked = null
                        scope.launch {
                            withContext(Dispatchers.IO) { Trash.restore(entry) }
                            reload()
                        }
                    }
                    TrashChoice(stringResource(R.string.trash_delete_forever)) {
                        picked = null
                        scope.launch {
                            withContext(Dispatchers.IO) { Trash.deleteForever(entry) }
                            reload()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashChoice(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Ink,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
    )
}
