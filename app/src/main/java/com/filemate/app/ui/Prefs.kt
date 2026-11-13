package com.filemate.app.ui

import android.content.Context
import android.content.SharedPreferences
import com.filemate.app.apk.SigningKeys
import com.filemate.app.data.FileRepository
import com.filemate.app.data.RootShell

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

    /** Warna tema yang dipilih di Pengaturan tema. */
    const val KEY_THEME = "theme_color"

    /** Perintah untuk mendapat akses root. Bawaannya "su". */
    const val KEY_SU = "su_command"

    /** Folder kerja Filemate (hasil bongkar APK, APK yang diambil, dan lainnya). */
    const val KEY_WORK_DIR = "work_dir"

    /** Gambar yang lebih besar dari ini (MB) tidak dibuatkan gambar mini. */
    const val KEY_THUMB_MAX_MB = "thumb_max_mb"

    /** Gambar mini yang belum selesai dimuat dalam waktu ini (detik) dibatalkan. */
    const val KEY_THUMB_TIMEOUT = "thumb_timeout"

    /** Berkas MD dan HTML dibuka sebagai pratinjau, bukan langsung di editor. */
    const val KEY_MD_PREVIEW = "md_preview"
    const val KEY_HTML_PREVIEW = "html_preview"

    /** Mode cairan: pewarnaan kode di editor dimatikan supaya lebih ringan. */
    const val KEY_FLUID = "editor_fluid"

    /** Editor teks: bungkus kata, pelengkap kode, ukuran huruf, dan nomor baris. */
    const val KEY_EDITOR_WRAP = "editor_wrap"
    const val KEY_EDITOR_COMPLETE = "editor_complete"
    const val KEY_EDITOR_FONT = "editor_font"
    const val KEY_EDITOR_NUMBERS = "editor_numbers"

    /** Folder awal panel kiri dan kanan. Kosong berarti mengikuti beranda. */
    const val KEY_HOME_LEFT = "home_left"
    const val KEY_HOME_RIGHT = "home_right"

    /** Nomor format tanggal di daftar berkas (lihat [DateFormats]). */
    const val KEY_DATE_FORMAT = "date_format"

    /** Menyembunyikan izin (rwx) di daftar berkas folder sistem. */
    const val KEY_HIDE_PERMS = "hide_perms"

    /** Waktu ubah berkas dipertahankan saat disalin. */
    const val KEY_KEEP_TIME = "keep_time"

    /** Gambar mini juga dimuat di flashdisk dan kartu SD. */
    const val KEY_THUMBS_EXTERNAL = "thumbs_external"

    /** Isi Recycle Bin yang lebih tua dari sekian hari dihapus otomatis. 0 berarti tidak pernah. */
    const val KEY_TRASH_DAYS = "trash_days"

    /** Memeriksa versi dan tanda tangan sebelum memasang APK, dan konfirmasi sebelum pasang lewat root. */
    const val KEY_VERIFY_INSTALL = "verify_install"
    const val KEY_INSTALL_CONFIRM = "install_confirm"

    /** Urutan pilihan menu tekan lama dan urutan cara buka di dialog "Buka dengan". */
    const val KEY_MENU_ORDER = "menu_order"
    const val KEY_OPEN_ORDER = "open_order"

    /** Berkas kunci tanda tangan pilihan user dan kata sandinya, serta daftar kata sandi umum. */
    const val KEY_SIGN_KEY = "sign_key"
    const val KEY_SIGN_PASS = "sign_pass"
    const val KEY_PASSWORDS = "common_passwords"

    /** Recycle Bin: fitur aktif, dicentang secara bawaan, dan peringatan merah saat hapus permanen. */
    const val KEY_TRASH_ON = "trash_enabled"
    const val KEY_TRASH_DEFAULT = "trash_default"
    const val KEY_DELETE_WARNING = "delete_warning"

    /** APK langsung ditandatangani setelah dirakit ulang. */
    const val KEY_AUTO_SIGN = "apk_auto_sign"

    /** APK dipasang lewat root (tanpa layar pemasang Android), dan nama paket yang dicatat sebagai sumbernya. */
    const val KEY_ROOT_INSTALL = "apk_root_install"
    const val KEY_INSTALLER = "apk_installer"

    /** minSdkVersion yang dipakai saat mengubah JAR menjadi DEX. */
    const val KEY_JAR_MIN_SDK = "jar_min_sdk"

    /**
     * Menerapkan pengaturan yang dipakai di banyak tempat: warna tema, perintah su, dan folder kerja.
     * Dipanggil sekali saat aplikasi atau layanan latar belakang mulai.
     */
    fun applyStartup(context: Context) {
        val prefs = of(context)
        ThemeColors.primary = prefs.getInt(KEY_THEME, ThemeColors.DEFAULT)
        RootShell.suCommand = prefs.getString(KEY_SU, "su").orEmpty().ifBlank { "su" }
        FileRepository.FILEMATE_DIR =
            prefs.getString(KEY_WORK_DIR, "").orEmpty().ifBlank { FileRepository.DEFAULT_FILEMATE_DIR }
        FileRepository.keepTime = prefs.getBoolean(KEY_KEEP_TIME, true)
        DateFormats.selected = prefs.getInt(KEY_DATE_FORMAT, 0)
        SigningKeys.customPath = prefs.getString(KEY_SIGN_KEY, "").orEmpty()
        SigningKeys.customPassword = prefs.getString(KEY_SIGN_PASS, "").orEmpty()
    }

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}
