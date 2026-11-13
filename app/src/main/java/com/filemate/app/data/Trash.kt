package com.filemate.app.data

import java.io.File

/** Satu berkas di Recycle Bin: nama aslinya, lokasi asalnya, dan berkas simpanannya. */
data class TrashEntry(val name: String, val originalPath: String, val stored: File)

/**
 * Recycle Bin: berkas yang dihapus dipindah ke folder tersembunyi di penyimpanan internal,
 * supaya bisa dipulihkan. Lokasi asal setiap berkas dicatat di berkas indeks.
 * Hanya dipakai untuk berkas di penyimpanan internal (tanpa root).
 */
object Trash {

    private val dir = File(FileRepository.STORAGE_ROOT, ".FilemateTrash")
    private val index = File(dir, ".index")

    /** True jika [path] bisa dipindah ke Recycle Bin (di penyimpanan internal dan bukan isi Recycle Bin sendiri). */
    fun supports(path: String): Boolean =
        path.startsWith(FileRepository.STORAGE_ROOT + "/") && !path.startsWith(dir.path)

    /** Memindah berkas atau folder ke Recycle Bin. */
    @Synchronized
    fun move(path: String): Boolean {
        val source = File(path)
        if (!source.exists()) return false
        dir.mkdirs()
        val stored = File(dir, "${System.currentTimeMillis()}_${source.name}")
        if (!source.renameTo(stored)) return false
        index.appendText("${stored.name}\t$path\n")
        return true
    }

    /** Isi Recycle Bin, yang terbaru di atas. */
    @Synchronized
    fun list(): List<TrashEntry> {
        if (!index.isFile) return emptyList()
        return index.readLines().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 2) return@mapNotNull null
            val stored = File(dir, parts[0])
            if (!stored.exists()) return@mapNotNull null
            TrashEntry(FileRepository.nameOf(parts[1]), parts[1], stored)
        }.reversed()
    }

    /** Mengembalikan berkas ke lokasi asalnya. Jika nama itu sudah dipakai, diberi nomor. */
    @Synchronized
    fun restore(entry: TrashEntry): Boolean {
        val parent = File(FileRepository.parentOf(entry.originalPath))
        parent.mkdirs()
        var target = File(entry.originalPath)
        var n = 2
        while (target.exists()) {
            val name = entry.name
            val dot = name.lastIndexOf('.')
            val numbered = if (dot > 0) "${name.substring(0, dot)} ($n)${name.substring(dot)}" else "$name ($n)"
            target = File(parent, numbered)
            n++
        }
        val ok = entry.stored.renameTo(target)
        if (ok) forget(entry)
        return ok
    }

    /** Menghapus berkas dari Recycle Bin untuk selamanya. */
    @Synchronized
    fun deleteForever(entry: TrashEntry): Boolean {
        val ok = entry.stored.deleteRecursively()
        if (ok) forget(entry)
        return ok
    }

    /**
     * Menghapus selamanya isi Recycle Bin yang sudah lebih dari [days] hari. Waktu dibuangnya
     * terbaca dari awalan nama berkas simpanan. Mengembalikan jumlah yang dihapus.
     */
    @Synchronized
    fun purgeOlderThan(days: Int): Int {
        if (days <= 0) return 0
        val limit = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        var removed = 0
        for (entry in list()) {
            val trashedAt = entry.stored.name.substringBefore('_').toLongOrNull() ?: continue
            if (trashedAt < limit && deleteForever(entry)) removed++
        }
        return removed
    }

    private fun forget(entry: TrashEntry) {
        if (!index.isFile) return
        val kept = index.readLines().filterNot { it.startsWith(entry.stored.name + "\t") }
        index.writeText(kept.joinToString("") { "$it\n" })
    }
}
