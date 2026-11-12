package com.filemate.app.ui

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.filemate.app.R
import com.filemate.app.data.FileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ukuran berkas dalam format singkat, misalnya 184 B, 1,2 KB, 12,4 MB. */
fun formatSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) {
        "$bytes B"
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[index])
    }
}

/** Tanggal dalam format yy-MM-dd HH:mm. */
fun formatDate(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("yy-MM-dd HH:mm", Locale.US).format(Date(millis))
}

/** Membuka berkas dengan aplikasi lain yang cocok di HP. */
fun openFile(context: Context, item: FileItem) {
    try {
        val file = File(item.path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(item.extension.lowercase()) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
    }
}
