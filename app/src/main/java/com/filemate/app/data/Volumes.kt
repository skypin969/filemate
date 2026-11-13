package com.filemate.app.data

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Satu penyimpanan lepas-pasang: flashdisk OTG atau kartu SD.
 * [needsRoot] true jika hanya bisa dibuka lewat akses root (jalur di bawah /mnt/media_rw).
 */
data class Volume(val path: String, val label: String, val needsRoot: Boolean)

/** Daftar flashdisk OTG dan kartu SD yang sedang terpasang. */
object Volumes {
    private val _list = MutableStateFlow<List<Volume>>(emptyList())
    val list: StateFlow<List<Volume>> = _list

    fun update(volumes: List<Volume>) {
        _list.value = volumes
    }

    /** Penyimpanan lepas-pasang yang memuat [path], atau null jika [path] bukan di dalamnya. */
    fun find(path: String): Volume? =
        _list.value.firstOrNull { path == it.path || path.startsWith(it.path + "/") }

    /** Nama penyimpanan jika [path] adalah folder paling atasnya. */
    fun labelFor(path: String): String? = _list.value.firstOrNull { it.path == path }?.label

    /** True jika [path] ada di flashdisk atau kartu SD yang bisa dibaca langsung tanpa root. */
    fun isDirect(path: String): Boolean = find(path)?.needsRoot == false

    /**
     * Mencari penyimpanan yang terpasang. Urutannya: daftar resmi dari Android, lalu isi folder
     * /storage, dan terakhir (jika [root] aktif) /mnt/media_rw untuk ROM yang tidak menampilkannya
     * ke aplikasi. Dipanggil di luar thread utama.
     */
    fun scan(context: Context, root: Boolean): List<Volume> {
        val found = LinkedHashMap<String, Volume>()
        try {
            val manager = context.getSystemService(StorageManager::class.java)
            for (volume in manager.storageVolumes) {
                if (volume.isPrimary) continue
                val mounted = volume.state == Environment.MEDIA_MOUNTED ||
                    volume.state == Environment.MEDIA_MOUNTED_READ_ONLY
                if (!mounted) continue
                val dir = volume.directory ?: continue
                val label = volume.getDescription(context)?.takeIf { it.isNotBlank() } ?: dir.name
                found[dir.name] = Volume(dir.path, label, needsRoot = false)
            }
        } catch (e: Exception) {
            // Lanjut ke cara berikutnya
        }
        try {
            File("/storage").listFiles()?.forEach { dir ->
                val special = dir.name == "emulated" || dir.name == "self"
                if (!special && dir.isDirectory && dir.canRead() && dir.name !in found) {
                    found[dir.name] = Volume(dir.path, dir.name, needsRoot = false)
                }
            }
        } catch (e: Exception) {
            // Lanjut ke cara berikutnya
        }
        if (root) {
            val result = RootShell.run("ls -1 /mnt/media_rw 2>/dev/null")
            if (result.exitCode == 0) {
                result.output.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { name ->
                    if (name !in found) found[name] = Volume("/mnt/media_rw/$name", name, needsRoot = true)
                }
            }
        }
        return found.values.toList()
    }
}
