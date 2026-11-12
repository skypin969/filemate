package com.filemate.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository

/**
 * Satu panel berkas: judul folder, baris ".." (jika ada folder induk),
 * lalu daftar isi yang sudah disaring. [visible] adalah hasil saringan dari ViewModel.
 */
@Composable
fun FilePanel(
    panel: PanelState,
    visible: List<FileItem>,
    isActive: Boolean,
    parentPath: String?,
    onActivate: () -> Unit,
    onOpenParent: () -> Unit,
    onTap: (FileItem) -> Unit,
    onLongPress: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = folderTitle(panel.path, stringResource(R.string.title_storage))

    Column(modifier = modifier.fillMaxHeight()) {
        // Garis biru di atas judul menandakan panel yang sedang aktif
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (isActive) Blue700 else Color.Transparent),
        )
        Text(
            text = title,
            color = if (isActive) Ink else Muted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(Lavender50)
                .clickable(onClick = onActivate)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )

        if (panel.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Blue700)
        }
        if (panel.error) {
            InfoText(stringResource(R.string.error_open_folder))
        } else if (!panel.loading && visible.isEmpty()) {
            InfoText(stringResource(R.string.empty_folder))
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (parentPath != null) {
                item(key = "parent") {
                    ParentRow(
                        onClick = {
                            onActivate()
                            onOpenParent()
                        },
                    )
                }
            }
            items(visible, key = { it.path }) { item ->
                FileRow(
                    item = item,
                    selected = item.path in panel.selected,
                    onClick = {
                        onActivate()
                        onTap(item)
                    },
                    onLongClick = { onLongPress(item) },
                )
            }
        }
    }
}

/** Judul folder. Folder penyimpanan utama diberi nama biasa, bukan angka "0". */
fun folderTitle(path: String, storageLabel: String): String = when (path) {
    FileRepository.STORAGE_ROOT -> storageLabel
    "/" -> "/"
    else -> FileRepository.nameOf(path).ifEmpty { "/" }
}

/** Baris ".." di paling atas daftar: naik satu folder. */
@Composable
private fun ParentRow(onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = FolderTint,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "..",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HorizontalDivider(color = LineColor)
    }
}

@Composable
private fun InfoText(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) Lavender100 else Color.Transparent)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileIcon(item)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = if (item.isApk) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle(item),
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Blue700,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(color = LineColor)
    }
}

@Composable
private fun FileIcon(item: FileItem) {
    if (item.isDir) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = FolderTint,
            modifier = Modifier.size(26.dp),
        )
    } else {
        val label = when {
            item.isApk -> "APK"
            item.extension.isEmpty() -> "?"
            else -> item.extension.take(3)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (item.isApk) Blue700 else BadgeGray),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (item.isApk) Color.White else BadgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun subtitle(item: FileItem): String {
    val date = formatDate(item.modified)
    return if (item.isDir) date else "${formatSize(item.size)} · $date"
}
