package com.filemate.app.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Peralatan berkas dari menu tekan lama: pisahkan, gabungkan, enkripsi, dekripsi,
 * tukar nama, dan ekspor daftar berkas. Semua bekerja dengan API Java biasa (tanpa root).
 */
object FileTools {

    /** Akhiran berkas hasil enkripsi. */
    const val ENCRYPTED_EXT = ".enc"

    private val MAGIC = "FMENC1".toByteArray(Charsets.US_ASCII)
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16
    private const val TAG_SIZE = 32
    private const val BUFFER = 64 * 1024

    /** True jika nama berkas adalah potongan hasil pisah, misalnya "video.mp4.001". */
    fun isPart(name: String): Boolean = Regex(".+\\.\\d{3}$").matches(name)

    /**
     * Memotong [file] menjadi beberapa berkas berukuran paling besar [partBytes]:
     * nama.001, nama.002, dan seterusnya, di folder yang sama. Mengembalikan jumlah potongan.
     */
    fun split(file: File, partBytes: Long): Int {
        require(partBytes > 0)
        val buffer = ByteArray(BUFFER)
        var index = 0
        FileInputStream(file).buffered().use { input ->
            var done = false
            while (!done) {
                index++
                val part = File(file.parentFile, String.format(Locale.US, "%s.%03d", file.name, index))
                var written = 0L
                FileOutputStream(part).buffered().use { out ->
                    while (written < partBytes) {
                        val want = minOf(buffer.size.toLong(), partBytes - written).toInt()
                        val n = input.read(buffer, 0, want)
                        if (n < 0) {
                            done = true
                            break
                        }
                        out.write(buffer, 0, n)
                        written += n
                    }
                }
                if (written == 0L) {
                    part.delete()
                    index--
                }
            }
        }
        return index
    }

    /**
     * Menyambung potongan nama.001, nama.002, ... menjadi satu berkas "nama" di folder yang sama.
     * [anyPart] boleh potongan mana saja. Mengembalikan berkas hasilnya, atau null jika potongan pertama tidak ada.
     */
    fun join(anyPart: File): File? {
        val base = anyPart.name.dropLast(4)
        val dir = anyPart.parentFile ?: return null
        if (!File(dir, "$base.001").isFile) return null
        val out = uniqueFile(dir, base)
        FileOutputStream(out).buffered().use { output ->
            var index = 1
            while (true) {
                val part = File(dir, String.format(Locale.US, "%s.%03d", base, index))
                if (!part.isFile) break
                FileInputStream(part).use { it.copyTo(output, BUFFER) }
                index++
            }
        }
        return out
    }

    /**
     * Mengenkripsi [src] ke [dest] dengan kata sandi. Isi diacak dengan AES-256 dan diberi
     * tanda keaslian, supaya kata sandi yang salah atau berkas yang rusak langsung ketahuan saat dibuka.
     */
    fun encrypt(src: File, dest: File, password: CharArray) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also { random.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { random.nextBytes(it) }
        val (encKey, macKey) = deriveKeys(password, salt)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encKey, IvParameterSpec(iv))
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(macKey)
        try {
            FileOutputStream(dest).buffered().use { out ->
                for (header in listOf(MAGIC, salt, iv)) {
                    out.write(header)
                    mac.update(header)
                }
                FileInputStream(src).buffered().use { input ->
                    val buffer = ByteArray(BUFFER)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        val chunk = cipher.update(buffer, 0, n)
                        if (chunk != null && chunk.isNotEmpty()) {
                            out.write(chunk)
                            mac.update(chunk)
                        }
                    }
                }
                val last = cipher.doFinal()
                if (last != null && last.isNotEmpty()) {
                    out.write(last)
                    mac.update(last)
                }
                out.write(mac.doFinal())
            }
        } catch (e: Exception) {
            dest.delete()
            throw e
        }
    }

    /**
     * Membuka berkas hasil [encrypt]. Mengembalikan false jika kata sandi salah, berkas rusak,
     * atau berkas itu bukan hasil enkripsi Filemate. Dalam hal itu [dest] tidak dibuat.
     */
    fun decrypt(src: File, dest: File, password: CharArray): Boolean {
        val headerSize = MAGIC.size + SALT_SIZE + IV_SIZE
        val bodySize = src.length() - headerSize - TAG_SIZE
        if (bodySize < 0) return false
        val magic = ByteArray(MAGIC.size)
        val salt = ByteArray(SALT_SIZE)
        val iv = ByteArray(IV_SIZE)

        // Langkah 1: periksa tanda keaslian seluruh berkas sebelum ada yang ditulis
        val keys = FileInputStream(src).buffered().use { input ->
            if (!readFully(input, magic) || !magic.contentEquals(MAGIC)) return false
            if (!readFully(input, salt) || !readFully(input, iv)) return false
            val derived = deriveKeys(password, salt)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(derived.second)
            mac.update(magic)
            mac.update(salt)
            mac.update(iv)
            val buffer = ByteArray(BUFFER)
            var left = bodySize
            while (left > 0) {
                val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                if (n < 0) return false
                mac.update(buffer, 0, n)
                left -= n
            }
            val tag = ByteArray(TAG_SIZE)
            if (!readFully(input, tag)) return false
            if (!MessageDigest.isEqual(tag, mac.doFinal())) return false
            derived
        }

        // Langkah 2: buka isinya
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keys.first, IvParameterSpec(iv))
        try {
            FileInputStream(src).buffered().use { input ->
                if (!readFully(input, ByteArray(headerSize))) return false
                FileOutputStream(dest).buffered().use { out ->
                    val buffer = ByteArray(BUFFER)
                    var left = bodySize
                    while (left > 0) {
                        val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                        if (n < 0) break
                        val chunk = cipher.update(buffer, 0, n)
                        if (chunk != null && chunk.isNotEmpty()) out.write(chunk)
                        left -= n
                    }
                    val last = cipher.doFinal()
                    if (last != null && last.isNotEmpty()) out.write(last)
                }
            }
        } catch (e: Exception) {
            dest.delete()
            throw e
        }
        return true
    }

    /** Menukar nama dua berkas atau folder. Keduanya boleh berada di folder yang berbeda. */
    fun swapNames(a: File, b: File): Boolean {
        val dirA = a.parentFile ?: return false
        val dirB = b.parentFile ?: return false
        if (a.name == b.name) return false
        val newA = File(dirA, b.name)
        val newB = File(dirB, a.name)
        val sameDir = dirA.absolutePath == dirB.absolutePath
        // Di folder berbeda, nama tujuan tidak boleh sudah dipakai berkas lain
        if (!sameDir && (newA.exists() || newB.exists())) return false
        val temp = File(dirA, ".tukar-" + System.nanoTime())
        if (!a.renameTo(temp)) return false
        if (!b.renameTo(newB)) {
            temp.renameTo(a)
            return false
        }
        if (!temp.renameTo(newA)) {
            newB.renameTo(b)
            temp.renameTo(a)
            return false
        }
        return true
    }

    /**
     * Menulis daftar nama semua berkas dan folder di dalam [roots] ke berkas teks [out],
     * satu baris per berkas. Mengembalikan jumlah baris.
     */
    fun exportList(roots: List<File>, out: File): Int {
        var count = 0
        out.bufferedWriter().use { writer ->
            for (root in roots) {
                val parent = root.parentFile
                for (file in root.walkTopDown()) {
                    val shown = if (parent == null) file.path else file.relativeTo(parent).invariantSeparatorsPath
                    writer.write(if (file.isDirectory) "$shown/" else shown)
                    writer.newLine()
                    count++
                }
            }
        }
        return count
    }

    /** Nama yang belum dipakai di [dir]: "nama", lalu "nama (2)", dan seterusnya (angka sebelum akhiran). */
    fun uniqueFile(dir: File, name: String): File {
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

    private fun deriveKeys(password: CharArray, salt: ByteArray): Pair<SecretKeySpec, SecretKeySpec> {
        val spec = PBEKeySpec(password, salt, 120_000, 512)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Pair(
            SecretKeySpec(bytes.copyOfRange(0, 32), "AES"),
            SecretKeySpec(bytes.copyOfRange(32, 64), "HmacSHA256"),
        )
    }

    private fun readFully(input: InputStream, target: ByteArray): Boolean {
        var offset = 0
        while (offset < target.size) {
            val n = input.read(target, offset, target.size - offset)
            if (n < 0) return false
            offset += n
        }
        return true
    }
}
