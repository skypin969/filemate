package com.filemate.app.data

/** Hasil satu perintah shell. */
data class ShellResult(val exitCode: Int, val output: String)

/**
 * Menjalankan perintah sebagai root lewat `su -c`.
 * Hanya berfungsi di HP yang sudah di-root (Magisk, KernelSU, dan sejenisnya).
 * Harus dipanggil dari thread latar belakang (Dispatchers.IO).
 */
object RootShell {

    fun run(command: String): ShellResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            ShellResult(code, output)
        } catch (e: Exception) {
            ShellResult(-1, e.message ?: "su tidak ditemukan")
        }
    }

    /** True jika perintah `su` diberi izin dan berjalan sebagai root (uid 0). */
    fun isGranted(): Boolean {
        val result = run("id -u")
        return result.exitCode == 0 && result.output.trim().lines().lastOrNull()?.trim() == "0"
    }

    /** Membungkus teks dengan tanda kutip tunggal supaya aman dipakai di shell. */
    fun quote(text: String): String = "'" + text.replace("'", "'\\''") + "'"
}
