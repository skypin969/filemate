package com.filemate.app.data

/** Satu berkas atau folder yang tampil di panel. */
data class FileItem(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val isLink: Boolean,
    val size: Long,
    val modified: Long,
) {
    val isApk: Boolean
        get() = !isDir && name.endsWith(".apk", ignoreCase = true)

    /** Ekstensi huruf besar, misalnya PDF. Kosong jika tidak ada titik. */
    val extension: String
        get() = name.substringAfterLast('.', "").uppercase()
}
