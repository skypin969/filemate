package com.filemate.app.apk

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.util.Collections
import java.util.zip.ZipFile

/**
 * Data untuk kartu info APK seperti di MT Manager. Semua dibaca dari berkas APK,
 * tanpa memasangnya.
 */
object ApkInfoReader {

    /** Rincian APK yang tampil di kartu. Nilainya kosong atau nol jika APK tidak bisa dibaca. */
    data class Summary(
        val label: String = "",
        val packageName: String = "",
        val versionName: String = "",
        val versionCode: Long = 0L,
        val size: Long = 0L,
        val signature: String = "",
        val protector: String? = null,
        val minSdk: Int = 0,
        val targetSdk: Int = 0,
    )

    /** Keadaan aplikasi di perangkat, jika sudah terpasang. */
    data class Installed(val path: String, val uid: Int)

    /** Pelindung yang dikenali dari nama library-nya di dalam APK. Hanya contoh yang umum. */
    private val PROTECTORS = listOf(
        "libjiagu" to "360 Jiagu",
        "libbaiduprotect" to "Baidu",
        "libsecexe" to "Bangcle",
        "libsecshell" to "Bangcle",
        "libshella" to "Tencent Legu",
        "libshell-super" to "Tencent Legu",
        "libnesec" to "NetEase",
    )

    /** Membaca rincian APK. Tidak melempar error: bagian yang gagal dibiarkan kosong. */
    fun read(context: Context, apk: File): Summary {
        val pm = context.packageManager
        val info = try {
            pm.getPackageArchiveInfo(apk.path, 0)
        } catch (e: Exception) {
            null
        }
        val app = info?.applicationInfo?.apply {
            // Tanpa dua baris ini, label tidak bisa diambil dari berkas APK di luar daftar aplikasi
            sourceDir = apk.path
            publicSourceDir = apk.path
        }
        return Summary(
            label = app?.let { runCatching { it.loadLabel(pm).toString() }.getOrNull() }.orEmpty(),
            packageName = info?.packageName.orEmpty(),
            versionName = info?.versionName.orEmpty(),
            versionCode = info?.longVersionCode ?: 0L,
            size = apk.length(),
            signature = ApkTools.signatureSchemes(apk),
            protector = protectorOf(apk),
            minSdk = app?.minSdkVersion ?: 0,
            targetSdk = app?.targetSdkVersion ?: 0,
        )
    }

    /** Keadaan aplikasi di perangkat, null jika belum terpasang. */
    fun installed(context: Context, packageName: String): Installed? {
        if (packageName.isEmpty()) return null
        return try {
            val app = context.packageManager.getPackageInfo(packageName, 0).applicationInfo ?: return null
            Installed(path = app.sourceDir.orEmpty(), uid = app.uid)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** Nama versi Android dari nomor API, misalnya 34 menjadi "14". Kosong jika nomornya tidak dikenal. */
    fun androidName(api: Int): String = when (api) {
        21 -> "5.0"
        22 -> "5.1"
        23 -> "6"
        24 -> "7"
        25 -> "7.1"
        26 -> "8"
        27 -> "8.1"
        28 -> "9"
        29 -> "10"
        30 -> "11"
        31 -> "12"
        32 -> "12L"
        33 -> "13"
        34 -> "14"
        35 -> "15"
        36 -> "16"
        else -> ""
    }

    /** Nama pelindung jika library-nya ditemukan di dalam APK, null jika tidak ada. */
    private fun protectorOf(apk: File): String? = try {
        ZipFile(apk).use { zip ->
            val names = Collections.list(zip.entries()).map { it.name.substringAfterLast('/').lowercase() }
            PROTECTORS.firstOrNull { (marker, _) -> names.any { it.startsWith(marker) } }?.second
        }
    } catch (e: Exception) {
        null
    }
}
