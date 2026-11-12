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

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
