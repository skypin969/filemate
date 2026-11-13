package com.filemate.app.ui

import android.content.ClipData
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

/** Ukuran berkas dalam format singkat seperti NP Manager, misalnya 184B, 1,20K, 12,40M, 9,10G. */
fun formatSize(bytes: Long): String {
    val units = listOf("B", "K", "M", "G")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) {
        "${bytes}B"
    } else {
        String.format(Locale.getDefault(), "%.2f%s", value, units[index])
    }
}

/** Tanggal dalam format yy-MM-dd HH:mm, dipakai di daftar berkas. */
fun formatDate(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("yy-MM-dd HH:mm", Locale.US).format(Date(millis))
}

/** Tanggal lengkap dengan detik, dipakai di dialog atribut. */
fun formatDateFull(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))
}

/** Jenis MIME dari akhiran berkas, dipakai saat membuka atau membagikan. */
private fun mimeOf(item: FileItem): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.extension.lowercase()) ?: "*/*"

/** Alamat aman untuk aplikasi lain, lewat FileProvider. */
private fun uriOf(context: Context, item: FileItem) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(item.path))

/** Membuka layar pemasang bawaan Android untuk sebuah APK. */
fun installWithSystem(context: Context, path: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
    }
}

/** Membuka berkas dengan aplikasi lain yang cocok di HP. */
fun openFile(context: Context, item: FileItem) {
    try {
        val uri = uriOf(context, item)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeOf(item))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
    }
}

/** Membuka berkas dengan aplikasi lain, dengan jenis MIME yang ditentukan (misalnya jenis gambar untuk penampil gambar). */
fun openFileAs(context: Context, item: FileItem, mime: String) {
    try {
        val uri = uriOf(context, item)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
    }
}

/** Mengirim berkas lewat aplikasi lain (WhatsApp, Gmail, Bluetooth, dan lainnya). */
fun shareFile(context: Context, item: FileItem) {
    try {
        val uri = uriOf(context, item)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeOf(item))
            .putExtra(Intent.EXTRA_STREAM, uri)
        // setClipData tidak mengembalikan Intent, jadi dipisah. Ini membuat izin baca ikut ke pilihan aplikasi.
        send.clipData = ClipData.newRawUri("", uri)
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, context.getString(R.string.menu_share)))
    } catch (e: Exception) {
        Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
    }
}

/** Memilih aplikasi yang akan dipakai untuk membuka berkas ini. */
fun openWith(context: Context, item: FileItem) {
    try {
        val uri = uriOf(context, item)
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeOf(item))
        view.clipData = ClipData.newRawUri("", uri)
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(view, context.getString(R.string.menu_open_with)))
    } catch (e: Exception) {
        Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
    }
}
