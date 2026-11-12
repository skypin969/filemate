package com.filemate.app.apk

import com.android.apksig.ApkSigner
import java.io.File
import java.util.Collections
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Alat APK yang berjalan di dalam aplikasi:
 * - [readManifest]: membaca AndroidManifest.xml sebagai teks
 * - [unpack]: membongkar isi APK ke sebuah folder
 * - [repack]: mengemas ulang folder menjadi APK (belum ditandatangani)
 * - [sign]: memberi tanda tangan pada APK
 *
 * Catatan: kode (dex) dan resource yang perlu dikompilasi ulang belum didukung.
 */
object ApkTools {

    fun readManifest(apk: File): String {
        ZipFile(apk).use { zip ->
            val entry = zip.getEntry("AndroidManifest.xml")
                ?: throw IllegalStateException("AndroidManifest.xml tidak ada")
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            return AxmlDecoder.decode(bytes)
        }
    }

    fun unpack(apk: File, outDir: File) {
        outDir.mkdirs()
        val base = outDir.canonicalFile
        ZipFile(apk).use { zip ->
            for (entry in Collections.list(zip.entries())) {
                val target = File(base, entry.name).canonicalFile
                // Cegah nama berkas yang keluar dari folder tujuan (path traversal)
                require(target.path == base.path || target.path.startsWith(base.path + File.separator)) {
                    "Nama berkas tidak aman: ${entry.name}"
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        val manifest = File(base, "AndroidManifest.xml")
        if (manifest.isFile) {
            File(base, "AndroidManifest.txt").writeText(AxmlDecoder.decode(manifest.readBytes()))
        }
    }

    fun repack(srcDir: File, outApk: File) {
        val root = srcDir.canonicalFile
        val files = root.walkTopDown()
            .filter { it.isFile }
            .map { it to it.relativeTo(root).invariantSeparatorsPath }
            .filterNot { (_, name) -> isSkipped(name) }
            .toList()

        outApk.parentFile?.mkdirs()
        ZipOutputStream(outApk.outputStream().buffered()).use { zos ->
            for ((file, name) in files) {
                val bytes = file.readBytes()
                val entry = ZipEntry(name)
                if (name == "resources.arsc") {
                    // Android mensyaratkan resources.arsc disimpan tanpa kompresi
                    entry.method = ZipEntry.STORED
                    entry.size = bytes.size.toLong()
                    entry.compressedSize = bytes.size.toLong()
                    entry.crc = CRC32().apply { update(bytes) }.value
                }
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }
        }
    }

    fun sign(input: File, output: File) {
        val key = SigningKeys.load()
        val signer = ApkSigner.SignerConfig.Builder(
            "FILEMATE",
            key.privateKey,
            listOf(key.certificate),
        ).build()

        output.parentFile?.mkdirs()
        ApkSigner.Builder(listOf(signer))
            .setInputApkFile(input)
            .setOutputApkFile(output)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }

    /** Tanda tangan lama dan file manifest buatan penanda tangan tidak ikut dikemas ulang. */
    private fun isSkipped(name: String): Boolean {
        if (name == "AndroidManifest.txt") return true
        if (!name.startsWith("META-INF/")) return false
        val upper = name.uppercase()
        return upper == "META-INF/MANIFEST.MF" ||
            upper.endsWith(".SF") ||
            upper.endsWith(".RSA") ||
            upper.endsWith(".DSA") ||
            upper.endsWith(".EC")
    }
}
