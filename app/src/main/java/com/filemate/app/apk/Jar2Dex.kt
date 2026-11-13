package com.filemate.app.apk

import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import java.io.File

/** Mengubah berkas JAR (kumpulan kelas Java) menjadi DEX yang bisa dijalankan Android, memakai D8. */
object Jar2Dex {

    const val DEFAULT_MIN_SDK = 13

    /**
     * Mengubah [jar] menjadi classes.dex di dalam [outDir]. [minSdk] adalah versi Android paling rendah
     * yang dituju (minSdkVersion). Melempar kesalahan jika JAR tidak bisa diubah.
     */
    fun convert(jar: File, outDir: File, minSdk: Int) {
        outDir.mkdirs()
        val command = D8Command.builder()
            .addProgramFiles(jar.toPath())
            .setMinApiLevel(minSdk)
            .setOutput(outDir.toPath(), OutputMode.DexIndexed)
            .build()
        D8.run(command)
    }
}
