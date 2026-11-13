package com.filemate.app.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Kompres dan ekstrak ZIP dengan API Java biasa (tanpa root). */
object Archive {

    /** Mengompres [source] (berkas atau folder) menjadi ZIP di [dest]. */
    fun zip(source: File, dest: File): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(dest).buffered()).use { out ->
                addEntry(out, source, source.name)
            }
            true
        } catch (e: Exception) {
            dest.delete()
            false
        }
    }

    /** Mengemas beberapa berkas atau folder sekaligus ke satu ZIP. */
    fun zipAll(sources: List<File>, dest: File): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(dest).buffered()).use { out ->
                for (source in sources) addEntry(out, source, source.name)
            }
            true
        } catch (e: Exception) {
            dest.delete()
            false
        }
    }

    private fun addEntry(out: ZipOutputStream, file: File, name: String) {
        if (file.isDirectory) {
            val children = file.listFiles().orEmpty()
            if (children.isEmpty()) {
                out.putNextEntry(ZipEntry("$name/"))
                out.closeEntry()
            }
            for (child in children) {
                addEntry(out, child, "$name/${child.name}")
            }
        } else {
            out.putNextEntry(ZipEntry(name))
            FileInputStream(file).use { input -> input.copyTo(out) }
            out.closeEntry()
        }
    }

    /**
     * Mengekstrak [zip] ke folder [destDir] (sebaiknya belum ada).
     * Entri yang mencoba keluar dari folder tujuan ditolak. Berkas yang bukan ZIP mengembalikan false.
     */
    fun unzip(zip: File, destDir: File): Boolean {
        return try {
            destDir.mkdirs()
            val base = destDir.canonicalPath + File.separator
            var count = 0
            ZipInputStream(FileInputStream(zip).buffered()).use { input ->
                var entry: ZipEntry? = input.nextEntry
                while (entry != null) {
                    val target = File(destDir, entry.name)
                    if (!target.canonicalPath.startsWith(base)) {
                        throw SecurityException("Entri ZIP tidak aman")
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                    count++
                    input.closeEntry()
                    entry = input.nextEntry
                }
            }
            count > 0
        } catch (e: Exception) {
            false
        }
    }

    // ---------- Menjelajah isi arsip seperti folder ----------

    /** Akhiran berkas arsip yang isinya bisa dibuka seperti folder. */
    val BROWSABLE = setOf("zip", "jar", "apk", "apks", "xapk", "aar")

    /**
     * Mencari apakah [path] menunjuk ke berkas arsip atau ke sesuatu di dalamnya.
     * Contoh: "/sdcard/a.zip/font/x.ttf" menghasilkan arsip "/sdcard/a.zip" dengan isi "font/x.ttf".
     * Null jika [path] berkas atau folder biasa.
     */
    fun locate(path: String): ArchiveLocation? {
        if (!path.startsWith("/")) return null
        var index = 0
        while (true) {
            val next = path.indexOf('/', index + 1)
            val end = if (next < 0) path.length else next
            val name = path.substring(index + 1, end)
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext in BROWSABLE) {
                val prefix = path.substring(0, end)
                if (File(prefix).isFile) {
                    val inner = if (next < 0) "" else path.substring(next + 1).trimEnd('/')
                    return ArchiveLocation(prefix, inner)
                }
            }
            if (next < 0) return null
            index = next
        }
    }

    /** True jika [path] berada di dalam sebuah arsip (bukan berkas arsipnya sendiri). */
    fun inside(path: String): Boolean = locate(path)?.inner?.isNotEmpty() == true

    /** Daftar isi satu tingkat folder di dalam arsip. */
    fun list(location: ArchiveLocation): Result<List<FileItem>> = try {
        val prefix = if (location.inner.isEmpty()) "" else location.inner + "/"
        val dirs = LinkedHashMap<String, Long>()
        val files = ArrayList<FileItem>()
        ZipFile(location.archive).use { zip ->
            for (entry in Collections.list(zip.entries())) {
                val name = entry.name.trimStart('/')
                if (!name.startsWith(prefix) || name.length == prefix.length) continue
                val rest = name.substring(prefix.length)
                val slash = rest.indexOf('/')
                if (slash >= 0) {
                    val dir = rest.substring(0, slash)
                    if (dir.isNotEmpty()) dirs[dir] = maxOf(dirs[dir] ?: 0L, entry.time)
                } else {
                    files.add(
                        FileItem(
                            name = rest,
                            path = location.archive + "/" + prefix + rest,
                            isDir = false,
                            isLink = false,
                            size = entry.size.coerceAtLeast(0L),
                            modified = entry.time,
                        ),
                    )
                }
            }
        }
        val folders = dirs.map { (name, time) ->
            FileItem(
                name = name,
                path = location.archive + "/" + prefix + name,
                isDir = true,
                isLink = false,
                size = 0L,
                modified = time,
            )
        }
        Result.success(folders + files)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mengeluarkan satu berkas atau satu folder dari dalam arsip ke [destDir].
     * Mengembalikan berkas atau folder hasilnya, atau null jika gagal.
     */
    fun extract(location: ArchiveLocation, destDir: File): File? {
        if (location.inner.isEmpty()) return null
        return try {
            destDir.mkdirs()
            val top = uniqueChild(destDir, location.inner.substringAfterLast('/'))
            ZipFile(location.archive).use { zip ->
                val entry = zip.getEntry(location.inner)
                if (entry != null && !entry.isDirectory) {
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(top).use { output -> input.copyTo(output) }
                    }
                } else {
                    val prefix = location.inner + "/"
                    top.mkdirs()
                    val base = top.canonicalPath + File.separator
                    for (child in Collections.list(zip.entries())) {
                        val name = child.name.trimStart('/')
                        if (!name.startsWith(prefix) || name.length == prefix.length) continue
                        val target = File(top, name.substring(prefix.length))
                        if (!(target.canonicalPath + File.separator).startsWith(base)) {
                            throw SecurityException("Entri ZIP tidak aman")
                        }
                        if (child.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            zip.getInputStream(child).use { input ->
                                FileOutputStream(target).use { output -> input.copyTo(output) }
                            }
                        }
                    }
                }
            }
            top
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mengeluarkan satu berkas dari arsip ke folder sementara di [cacheDir] supaya bisa dibuka.
     * Salinan lama dari berkas yang sama ditimpa.
     */
    fun extractToTemp(location: ArchiveLocation, cacheDir: File): File? {
        val folder = File(cacheDir, "arsip/" + Integer.toHexString((location.archive + "|" + location.inner).hashCode()))
        folder.deleteRecursively()
        return extract(location, folder)
    }

    private fun uniqueChild(dir: File, name: String): File {
        var candidate = File(dir, name)
        var n = 2
        while (candidate.exists()) {
            val dot = name.lastIndexOf('.')
            val numbered = if (dot > 0) "${name.substring(0, dot)} ($n)${name.substring(dot)}" else "$name ($n)"
            candidate = File(dir, numbered)
            n++
        }
        return candidate
    }
}

/** Lokasi di dalam arsip: [archive] berkas arsipnya, [inner] jalur di dalamnya ("" berarti paling atas). */
data class ArchiveLocation(val archive: String, val inner: String)
