package com.filemate.app.ui

import android.content.Context
import android.content.SharedPreferences

/** Nama dan kunci pengaturan yang disimpan di HP. Dipakai layar utama, pengaturan, dan editor. */
object AppPrefs {
    const val NAME = "filemate"
    const val KEY_BOOKMARKS = "bookmarks"

    /** Folder yang dibuka saat aplikasi mulai. Kosong berarti penyimpanan internal. */
    const val KEY_HOME = "home_path"

    /** Editor menyimpan berkas asli sebagai .bak sebelum menimpanya. */
    const val KEY_BACKUP = "editor_backup"

    /** Port layanan APK MCP. */
    const val KEY_MCP_PORT = "mcp_port"

    /** Layanan APK MCP dimulai otomatis saat aplikasi dibuka. */
    const val KEY_MCP_AUTO = "mcp_auto_start"

    /** Layanan APK MCP boleh dihubungi dari perangkat lain di Wi-Fi yang sama. */
    const val KEY_MCP_LAN = "mcp_allow_lan"

    /** Ukuran daftar berkas: 0 kecil, 1 sedang, 2 besar. */
    const val KEY_LIST_SIZE = "list_size"

    /** Jumlah baris paling banyak untuk nama berkas di daftar. */
    const val KEY_NAME_LINES = "name_lines"

    /** Jarak tambahan di bawah bilah tombol bawah, dalam dp. */
    const val KEY_BOTTOM_MARGIN = "bottom_margin"

    /** Menampilkan gambar mini untuk berkas gambar. */
    const val KEY_THUMBS = "thumbnails"

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
