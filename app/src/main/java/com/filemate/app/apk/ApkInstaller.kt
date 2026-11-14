package com.filemate.app.apk

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.filemate.app.R
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Hasil pemasangan APK yang dikirim sistem ke [ApkInstallReceiver]. */
sealed interface ApkInstallEvent {
    data object Success : ApkInstallEvent
    data class Failed(val message: String) : ApkInstallEvent
}

/** Tempat hasil pemasangan lewat. Receiver mengirim ke sini, ViewModel yang mendengarkan. */
object ApkInstallEvents {
    private val flow = MutableSharedFlow<ApkInstallEvent>(extraBufferCapacity = 8)

    val events: SharedFlow<ApkInstallEvent> = flow.asSharedFlow()

    fun post(event: ApkInstallEvent) {
        flow.tryEmit(event)
    }
}

/** Memasang APK lewat sesi pemasang sistem. Hasilnya datang lewat [ApkInstallReceiver]. */
object ApkInstaller {

    /** Menyalin APK ke sesi pemasang lalu menjalankannya. Sebaiknya dipanggil dari thread latar. */
    fun start(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val sessionId = installer.createSession(
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL),
        )
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, apk.length()).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                val intent = Intent(context, ApkInstallReceiver::class.java)
                val pending = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pending.intentSender)
            }
        } catch (e: Exception) {
            installer.abandonSession(sessionId)
            throw e
        }
    }
}

/** Menerima status pemasangan dari sistem. Jika sistem minta persetujuan, layar konfirmasinya dibuka. */
class ApkInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> ApkInstallEvents.post(ApkInstallEvent.Success)
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                ApkInstallEvents.post(ApkInstallEvent.Failed(context.getString(R.string.apk_install_aborted)))
            }
            else -> {
                val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.apk_install_code, status)
                ApkInstallEvents.post(ApkInstallEvent.Failed(detail))
            }
        }
    }
}
