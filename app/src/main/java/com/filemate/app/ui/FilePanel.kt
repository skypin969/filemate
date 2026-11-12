package com.filemate.app.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.filemate.app.R
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Satu panel berkas, tampilannya mengikuti NP Manager: baris ".." (jika ada folder induk),
 * lalu daftar isi yang rapat tanpa garis pemisah. [visible] adalah hasil saringan dari ViewModel.
 */
@Composable
fun FilePanel(
    panel: PanelState,
    visible: List<FileItem>,
    parentPath: String?,
    onActivate: () -> Unit,
    onOpenParent: () -> Unit,
    onTap: (FileItem) -> Unit,
    onIconTap: (FileItem) -> Unit,
    onLongPress: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onActivate),
    ) {
        if (panel.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NpBlue)
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
            if (panel.error) {
                item(key = "error") { InfoText(stringResource(R.string.error_open_folder)) }
            } else if (!panel.loading && visible.isEmpty()) {
                item(key = "empty") { InfoText(stringResource(R.string.empty_folder)) }
            }
            items(visible, key = { it.path }) { item ->
                FileRow(
                    item = item,
                    selected = item.path in panel.selected,
                    onClick = {
                        onActivate()
                        onTap(item)
                    },
                    onIconClick = { onIconTap(item) },
                    onLongClick = {
                        onActivate()
                        onLongPress(item)
                    },
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = NpBlue,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = "..", color = Ink, fontSize = 15.sp)
    }
}

@Composable
private fun InfoText(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onIconClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) SelectTint else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mengetuk ikon memilih berkas ini (bisa beberapa sekaligus), seperti NP Manager
        Box(modifier = Modifier.clickable(onClick = onIconClick)) {
            FileIcon(item)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle(item),
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Tautan (symlink) diberi tanda panah di kanan, seperti NP Manager
        if (item.isLink) {
            Text(text = "->", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FileIcon(item: FileItem) {
    when {
        item.isDir -> Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = NpBlue,
            modifier = Modifier.size(30.dp),
        )

        item.isApk -> ApkIcon(item.path)

        else -> Icon(
            imageVector = Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = FileTint,
            modifier = Modifier.size(30.dp),
        )
    }
}

// Ikon APK yang sudah pernah dimuat, supaya tidak dimuat ulang setiap daftar digulir
private val apkIconCache = ConcurrentHashMap<String, ImageBitmap>()

/** Ikon asli dari dalam berkas APK. Selama belum termuat, tampil ikon Android biasa. */
@Composable
private fun ApkIcon(path: String) {
    val context = LocalContext.current
    val bitmap by produceState(initialValue = apkIconCache[path], path) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { loadApkIcon(context, path) }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Android,
            contentDescription = null,
            tint = Color(0xFF3DDC84),
            modifier = Modifier.size(30.dp),
        )
    }
}

private fun loadApkIcon(context: Context, path: String): ImageBitmap? = try {
    val pm = context.packageManager
    val app = pm.getPackageArchiveInfo(path, 0)?.applicationInfo
    if (app == null) {
        null
    } else {
        // Tanpa dua baris ini, Android tidak tahu ikon harus diambil dari berkas APK yang mana
        app.sourceDir = path
        app.publicSourceDir = path
        val image = app.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
        if (apkIconCache.size > 200) apkIconCache.clear()
        apkIconCache[path] = image
        image
    }
} catch (e: Exception) {
    null
}

private fun subtitle(item: FileItem): String {
    val date = formatDate(item.modified)
    return if (item.isDir) date else "$date  ${formatSize(item.size)}"
}
