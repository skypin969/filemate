package com.filemate.app.data

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
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
     * ke aplikasi. Cara kedua dan ketiga hanya dipakai jika flashdisk OTG benar-benar tersambung,
     * dan folder yang dipakai harus titik pasang yang aktif. Folder kosong sisa flashdisk yang sudah
     * dicabut diabaikan. Dipanggil di luar thread utama.
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
        if (usbStorageConnected(context)) {
            val mounts = mountTypes(root)
            try {
                File("/storage").listFiles()?.forEach { dir ->
                    val special = dir.name == "emulated" || dir.name == "self"
                    if (!special && dir.isDirectory && dir.canRead() && dir.name !in found &&
                        isActiveMount(mounts, dir.path)
                    ) {
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
                        val path = "/mnt/media_rw/$name"
                        if (name !in found && isActiveMount(mounts, path)) {
                            found[name] = Volume(path, name, needsRoot = true)
                        }
                    }
                }
            }
        }
        return found.values.toList()
    }

    /** True jika ada perangkat penyimpanan USB (flashdisk) yang sedang tersambung lewat OTG. */
    private fun usbStorageConnected(context: Context): Boolean {
        val usb = context.getSystemService(UsbManager::class.java) ?: return false
        return usb.deviceList.values.any { device ->
            (0 until device.interfaceCount).any { i ->
                device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
            }
        }
    }

    /**
     * Titik pasang yang sedang aktif beserta jenis filesystem-nya, dibaca dari /proc/mounts.
     * Jika [root] aktif, dibaca lewat akses root agar /mnt/media_rw ikut terbaca.
     */
    private fun mountTypes(root: Boolean): Map<String, String> {
        val rootText = if (root) {
            RootShell.run("cat /proc/mounts 2>/dev/null").takeIf { it.exitCode == 0 }?.output
        } else {
            null
        }
        val text = rootText ?: try {
            File("/proc/mounts").readText()
        } catch (e: Exception) {
            ""
        }
        val types = HashMap<String, String>()
        text.lineSequence().forEach { line ->
            val parts = line.split(' ')
            if (parts.size >= 3) {
                val point = parts[1]
                    .replace("\\040", " ")
                    .replace("\\011", "\t")
                    .replace("\\012", "\n")
                    .replace("\\134", "\\")
                types[point] = parts[2]
            }
        }
        return types
    }

    /** True jika [path] adalah titik pasang yang aktif. Folder biasa atau tmpfs (RAM) tidak dihitung. */
    private fun isActiveMount(mounts: Map<String, String>, path: String): Boolean {
        val type = mounts[path] ?: return false
        return type != "tmpfs"
    }
}
