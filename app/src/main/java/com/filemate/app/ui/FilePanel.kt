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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assignment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.filemate.app.R
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import com.filemate.app.data.Volumes
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    style: ListStyle,
    modifier: Modifier = Modifier,
    fresh: Set<String> = emptySet(),
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onActivate),
    ) {
        if (panel.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (parentPath != null) {
                item(key = "parent") {
                    ParentRow(
                        style = style,
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
                    fresh = item.path in fresh,
                    style = style,
                    // Di folder data aplikasi, folder bernama paket diberi ikon aplikasinya
                    packageFolders = isPackageParent(panel.path),
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
    // Folder paling atas flashdisk atau kartu SD diberi namanya, bukan kode seperti "1A2B-3C4D"
    else -> Volumes.labelFor(path) ?: FileRepository.nameOf(path).ifEmpty { "/" }
}

/**
 * Pilihan tampilan daftar dari halaman pengaturan: [scale] untuk ukuran (kecil, sedang, besar),
 * [nameLines] untuk jumlah baris nama berkas, dan [thumbnails] untuk gambar mini.
 */
data class ListStyle(
    val scale: Float = 1f,
    val nameLines: Int = 3,
    val thumbnails: Boolean = true,
    /** Gambar yang lebih besar dari ini (byte) tidak dibuatkan gambar mini. */
    val thumbMaxBytes: Long = 10L * 1024 * 1024,
    /** Gambar mini yang belum selesai dalam waktu ini (milidetik) dibatalkan. */
    val thumbTimeoutMs: Long = 10_000L,
)

/** Folder yang isinya folder bernama paket aplikasi. */
private fun isPackageParent(path: String): Boolean =
    path.endsWith("/Android/data") || path.endsWith("/Android/obb") || path.endsWith("/Android/media") ||
        path == "/data/data" || path == "/data/user/0"

/** Baris ".." di paling atas daftar: naik satu folder. */
@Composable
private fun ParentRow(style: ListStyle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = (40 * style.scale).dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = FolderColor,
            modifier = Modifier.size((30 * style.scale).dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = "..", color = Ink, fontSize = (15 * style.scale).sp)
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
    fresh: Boolean,
    style: ListStyle,
    packageFolders: Boolean,
    onClick: () -> Unit,
    onIconClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val recent = !item.isDir && System.currentTimeMillis() - item.modified in 0 until RECENT_MILLIS
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) SelectTint else Color.Transparent)
            // Geser baris ke kiri atau kanan untuk menandai berkas, sama seperti mengetuk ikonnya
            .swipeSelect(item.path, onIconClick)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = (40 * style.scale).dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mengetuk ikon memilih berkas ini (bisa beberapa sekaligus), seperti NP Manager
        Box(modifier = Modifier.clickable(onClick = onIconClick)) {
            FileIcon(item, style, packageFolders)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                // Berkas yang baru disalin atau dipindah diberi warna hijau, seperti MT Manager
                color = if (fresh) FreshGreen else Ink,
                fontWeight = if (recent) FontWeight.Bold else FontWeight.Normal,
                fontSize = (15 * style.scale).sp,
                lineHeight = (18 * style.scale).sp,
                maxLines = style.nameLines,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle(item),
                color = Muted,
                fontSize = (11 * style.scale).sp,
                lineHeight = (13 * style.scale).sp,
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
private fun FileIcon(item: FileItem, style: ListStyle, packageFolders: Boolean) {
    val size = (30 * style.scale).dp
    when {
        item.isDir -> Box {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = FolderColor,
                modifier = Modifier.size(size),
            )
            // Ikon aplikasi kecil di pojok kanan bawah folder, seperti NP Manager
            if (packageFolders) {
                PackageBadge(
                    packageName = item.name,
                    modifier = Modifier.align(Alignment.BottomEnd).size(size * 0.55f),
                )
            }
        }

        item.isApk -> ApkIcon(item.path, size)

        style.thumbnails && item.extension in IMAGE_EXT && item.size <= style.thumbMaxBytes ->
            ImageThumb(item.path, size, style.thumbTimeoutMs)

        // Berkas teks dan arsip punya bentuk ikon sendiri, warnanya tetap abu-abu gelap (tema hitam putih)
        item.extension in TEXT_EXT -> Icon(
            imageVector = Icons.Filled.Assignment,
            contentDescription = null,
            tint = MenuIcon,
            modifier = Modifier.size(size),
        )

        item.extension in ARCHIVE_EXT -> Icon(
            imageVector = Icons.Filled.Archive,
            contentDescription = null,
            tint = MenuIcon,
            modifier = Modifier.size(size),
        )

        else -> Icon(
            imageVector = Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = FileTint,
            modifier = Modifier.size(size),
        )
    }
}

private val IMAGE_EXT = setOf("JPG", "JPEG", "PNG", "WEBP", "GIF", "BMP")

// Gambar mini dan ikon aplikasi yang sudah pernah dimuat, supaya tidak dimuat ulang setiap daftar digulir
private val thumbCache = ConcurrentHashMap<String, ImageBitmap>()
private val packageIconCache = ConcurrentHashMap<String, ImageBitmap>()

/** Gambar mini sebuah berkas gambar. Gambar dibaca dalam ukuran kecil supaya hemat memori. */
@Composable
private fun ImageThumb(path: String, size: Dp, timeoutMs: Long) {
    val bitmap by produceState(initialValue = thumbCache[path], path) {
        if (value == null) {
            // Jika terlalu lama (misalnya berkas rusak), pemuatan dilepas dan ikon biasa yang tampil
            value = withTimeoutOrNull(timeoutMs) { withContext(Dispatchers.IO) { loadThumb(path) } }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(RoundedCornerShape(4.dp)),
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = null,
            tint = FileTint,
            modifier = Modifier.size(size),
        )
    }
}

private fun loadThumb(path: String): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    // Diperkecil dengan kelipatan dua sampai sisi terpendeknya sekitar 128 piksel
    while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 128) sample *= 2
    val image = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    if (image != null) {
        if (thumbCache.size > 300) thumbCache.clear()
        thumbCache[path] = image
    }
    image
} catch (e: Throwable) {
    null
}

/** Ikon aplikasi yang nama paketnya sama dengan nama folder. Tidak tampil jika aplikasinya tidak terpasang. */
@Composable
private fun PackageBadge(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState(initialValue = packageIconCache[packageName], packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val image = context.packageManager.getApplicationIcon(packageName).toBitmap(72, 72).asImageBitmap()
                    packageIconCache[packageName] = image
                    image
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(bitmap = loaded, contentDescription = null, modifier = modifier)
    }
}

/** Akhiran berkas yang dianggap teks: dibuka langsung di editor teks dan diberi ikon papan catatan. */
val TEXT_EXT = setOf(
    "TXT", "MD", "XML", "JSON", "HTML", "HTM", "CSS", "JS", "SH", "PROP", "CONF", "CFG", "INI", "LOG", "RC",
    "JAVA", "KT", "KTS", "SMALI", "YML", "YAML", "CSV", "SVG", "PY", "C", "H", "CPP", "GRADLE", "PROPERTIES", "BAT",
)

/** Akhiran berkas font yang dibuka dengan penampil font. */
val FONT_EXT = setOf("TTF", "OTF", "TTC")

/** Warna nama berkas yang baru disalin atau dipindah. */
private val FreshGreen = Color(0xFF2E7D32)

private val ARCHIVE_EXT = setOf("ZIP", "RAR", "7Z", "TAR", "GZ", "TGZ", "XZ", "BZ2", "JAR", "APKS", "XAPK")

/** Berkas yang baru dibuat atau diubah (30 menit terakhir) namanya dicetak tebal. */
private const val RECENT_MILLIS = 30L * 60 * 1000

// Ikon APK yang sudah pernah dimuat, supaya tidak dimuat ulang setiap daftar digulir
private val apkIconCache = ConcurrentHashMap<String, ImageBitmap>()

/** Ikon asli dari dalam berkas APK. Selama belum termuat, tampil ikon Android biasa. */
@Composable
private fun ApkIcon(path: String, size: Dp) {
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
            modifier = Modifier.size(size),
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Android,
            contentDescription = null,
            tint = MenuIcon,
            modifier = Modifier.size(size),
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
