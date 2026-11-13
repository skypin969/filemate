package com.filemate.app.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Semua operasi berkas. Setiap fungsi punya parameter [root]:
 * - false: memakai API Java biasa (hanya folder yang diizinkan Android)
 * - true: memakai perintah shell sebagai root (bisa folder sistem)
 */
object FileRepository {

    const val STORAGE_ROOT = "/storage/emulated/0"
    const val DEFAULT_FILEMATE_DIR = "/storage/emulated/0/Filemate"

    /** Folder kerja Filemate. Bisa diganti di Pengaturan. */
    @Volatile
    var FILEMATE_DIR: String = DEFAULT_FILEMATE_DIR

    // Baris keluaran `ls -la`, contoh: -rw-rw---- 1 root sdcard_rw 1234 2026-11-12 10:00 nama.txt
    // Kolom: izin, jumlah link, pemilik, grup, ukuran, tanggal, nama
    private val lsLine = Regex(
        "^([-bcdlps][rwxsStTl-]{9}[.+@]?)\\s+\\S+\\s+\\S+\\s+\\S+\\s+(\\d+)\\s+" +
            "(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}|[A-Za-z]{3}\\s+\\d{1,2}\\s+(?:\\d{2}:\\d{2}|\\d{4}))\\s+(.+)$"
    )

    // ---------- Membaca isi folder ----------

    fun listLocal(dir: String): Result<List<FileItem>> {
        // Android tidak mengizinkan isi /storage/emulated dibaca, padahal isinya hanya folder "0"
        // (penyimpanan internal). Folder itu ditampilkan langsung supaya bisa naik-turun seperti NP Manager.
        if (dir == "/storage/emulated") {
            val zero = File(STORAGE_ROOT)
            return Result.success(
                listOf(FileItem("0", STORAGE_ROOT, isDir = true, isLink = false, size = 0L, modified = zero.lastModified())),
            )
        }
        val children = File(dir).listFiles()
            ?: return Result.failure(IllegalStateException("Folder tidak bisa dibuka"))
        val items = children.map { f ->
            FileItem(
                name = f.name,
                path = f.absolutePath,
                isDir = f.isDirectory,
                isLink = false,
                size = if (f.isDirectory) 0L else f.length(),
                modified = f.lastModified(),
            )
        }
        return Result.success(items)
    }

    /**
     * Membaca isi folder lewat root. [shownDir] adalah alamat yang dipakai untuk hasilnya,
     * jika berbeda dari alamat yang dibaca (lihat [listProtected]).
     */
    fun listRoot(dir: String, shownDir: String = dir): Result<List<FileItem>> {
        val result = RootShell.run("ls -la ${RootShell.quote(dir)}")
        if (result.exitCode != 0) {
            return Result.failure(IllegalStateException(result.output))
        }
        val items = result.output.lineSequence()
            .mapNotNull { parseLsLine(it.trimEnd(), shownDir) }
            .toList()
        return Result.success(items)
    }

    /** True untuk Android/data dan Android/obb, yang isinya disembunyikan Android dari aplikasi biasa. */
    fun isProtectedStorage(path: String): Boolean =
        path.startsWith("$STORAGE_ROOT/Android/data") || path.startsWith("$STORAGE_ROOT/Android/obb")

    /**
     * Membaca Android/data atau Android/obb lewat root. Jika alamat biasa tidak memberi hasil,
     * dicoba lewat alamat asli penyimpanan (/data/media/0), dan hasilnya tetap ditampilkan dengan alamat biasa.
     */
    fun listProtected(dir: String): Result<List<FileItem>> {
        val direct = listRoot(dir)
        if (direct.isSuccess && direct.getOrDefault(emptyList()).isNotEmpty()) return direct
        val real = "/data/media/0" + dir.removePrefix(STORAGE_ROOT)
        val viaReal = listRoot(real, shownDir = dir)
        return if (viaReal.isSuccess) viaReal else direct
    }

    private fun parseLsLine(line: String, dir: String): FileItem? {
        val match = lsLine.find(line) ?: return null
        val perm = match.groupValues[1]
        var name = match.groupValues[4]
        val isLink = perm.first() == 'l'
        if (isLink && name.contains(" -> ")) name = name.substringBefore(" -> ")
        if (name == "." || name == "..") return null
        return FileItem(
            name = name,
            path = join(dir, name),
            isDir = perm.first() == 'd',
            isLink = isLink,
            size = match.groupValues[2].toLongOrNull() ?: 0L,
            modified = parseLsDate(match.groupValues[3]),
        )
    }

    private fun parseLsDate(text: String): Long {
        val patterns = listOf("yyyy-MM-dd HH:mm", "MMM d HH:mm", "MMM d yyyy")
        for (pattern in patterns) {
            val parsed = (try {
                SimpleDateFormat(pattern, Locale.US).parse(text)
            } catch (e: Exception) {
                null
            }) ?: continue
            if (pattern == "MMM d HH:mm") {
                // Format ini tidak menulis tahun, jadi pakai tahun sekarang
                val cal = Calendar.getInstance().apply { time = parsed }
                cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                return cal.timeInMillis
            }
            return parsed.time
        }
        return 0L
    }

    // ---------- Membuat, mengubah nama, dan menghapus ----------

    fun createFolder(parent: String, name: String, root: Boolean): Boolean {
        return if (root) {
            RootShell.run("mkdir ${RootShell.quote(join(parent, name))}").exitCode == 0
        } else {
            File(parent, name).mkdir()
        }
    }

    /** Membuat berkas kosong. */
    fun createFile(parent: String, name: String, root: Boolean): Boolean {
        return if (root) {
            RootShell.run("touch ${RootShell.quote(join(parent, name))}").exitCode == 0
        } else {
            try {
                File(parent, name).createNewFile()
            } catch (e: Exception) {
                false
            }
        }
    }

    fun rename(path: String, newName: String, root: Boolean): Boolean {
        val target = join(parentOf(path), newName)
        return if (root) {
            RootShell.run("mv ${RootShell.quote(path)} ${RootShell.quote(target)}").exitCode == 0
        } else {
            File(path).renameTo(File(target))
        }
    }

    fun delete(paths: List<String>, root: Boolean): Boolean {
        return if (root) {
            val args = paths.joinToString(" ") { RootShell.quote(it) }
            RootShell.run("rm -rf $args").exitCode == 0
        } else {
            paths.map { File(it).deleteRecursively() }.all { it }
        }
    }

    /** Menyalin atau memindahkan [src] ke folder [destDir]. Nama dibuat unik jika sudah ada. */
    fun transfer(src: String, destDir: String, move: Boolean, root: Boolean): Boolean {
        val target = uniqueTarget(destDir, nameOf(src), root)
        if (root) {
            val verb = if (move) "mv" else "cp -r"
            return RootShell.run("$verb ${RootShell.quote(src)} ${RootShell.quote(target)}").exitCode == 0
        }
        val source = File(src)
        val dest = File(target)
        if (move && source.renameTo(dest)) return true
        return try {
            source.copyRecursively(dest, overwrite = false)
            if (move) source.deleteRecursively() else true
        } catch (e: Exception) {
            false
        }
    }

    fun exists(path: String, root: Boolean): Boolean {
        return if (root) {
            RootShell.run("if [ -e ${RootShell.quote(path)} ]; then echo 1; else echo 0; fi")
                .output.trim().endsWith("1")
        } else {
            File(path).exists()
        }
    }

    private fun uniqueTarget(dir: String, name: String, root: Boolean): String {
        var candidate = join(dir, name)
        var i = 2
        while (exists(candidate, root)) {
            val dot = name.lastIndexOf('.')
            val newName = if (dot > 0) {
                "${name.substring(0, dot)} ($i)${name.substring(dot)}"
            } else {
                "$name ($i)"
            }
            candidate = join(dir, newName)
            i++
        }
        return candidate
    }

    // ---------- Bantuan path ----------

    fun join(dir: String, name: String): String =
        if (dir.endsWith("/")) dir + name else "$dir/$name"

    fun parentOf(path: String): String {
        val parent = path.substringBeforeLast('/', "")
        return if (parent.isEmpty()) "/" else parent
    }

    fun nameOf(path: String): String = path.substringAfterLast('/')

    /** Folder sistem: bukan penyimpanan pengguna (/storage, /sdcard, /data/media). */
    fun isSystemPath(path: String): Boolean =
        !(path.startsWith("/storage/") || path.startsWith("/sdcard") || path.startsWith("/data/media/"))
}
