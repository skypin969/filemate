package com.filemate.app.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
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
}
