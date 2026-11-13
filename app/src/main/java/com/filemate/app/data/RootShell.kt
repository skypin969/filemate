package com.filemate.app.data

/** Hasil satu perintah shell. */
data class ShellResult(val exitCode: Int, val output: String)

/**
 * Menjalankan perintah sebagai root lewat `su -c`.
 * Hanya berfungsi di HP yang sudah di-root (Magisk, KernelSU, dan sejenisnya).
 * Harus dipanggil dari thread latar belakang (Dispatchers.IO).
 */
object RootShell {

    /** Perintah untuk mendapat akses root. Bisa diganti di Pengaturan, misalnya "su --mount-master". */
    @Volatile
    var suCommand: String = "su"

    /** Perintah su dipecah per kata, siap dipakai untuk menjalankan proses. */
    fun suArgs(): List<String> =
        suCommand.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.ifEmpty { listOf("su") }

    fun run(command: String): ShellResult {
        return try {
            val process = ProcessBuilder(suArgs() + listOf("-c", command))
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            ShellResult(code, output)
        } catch (e: Exception) {
            ShellResult(-1, e.message ?: "su tidak ditemukan")
        }
    }

    /** Menjalankan perintah sebagai aplikasi biasa (tanpa root) lewat `sh -c`. */
    fun sh(command: String): ShellResult {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            ShellResult(code, output)
        } catch (e: Exception) {
            ShellResult(-1, e.message ?: "sh tidak bisa dijalankan")
        }
    }

    /** Memilih root atau shell biasa sesuai [root]. */
    fun exec(command: String, root: Boolean): ShellResult = if (root) run(command) else sh(command)

    /** True jika perintah `su` diberi izin dan berjalan sebagai root (uid 0). */
    fun isGranted(): Boolean {
        val result = run("id -u")
        return result.exitCode == 0 && result.output.trim().lines().lastOrNull()?.trim() == "0"
    }

    /** Membungkus teks dengan tanda kutip tunggal supaya aman dipakai di shell. */
    fun quote(text: String): String = "'" + text.replace("'", "'\\''") + "'"
}
