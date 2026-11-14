package com.filemate.app.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.filemate.app.MainActivity
import com.filemate.app.R
import com.filemate.app.data.FileRepository
import java.io.File
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Layanan latar depan serbaguna: selama ada pekerjaan latar belakang (server FTP, HTTP, dan Transfer LAN),
 * layanan ini menjaga Filemate tidak dihentikan Android. Tiap pekerjaan mendaftar dengan sebuah nama.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.keep_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.keep_title))
            .setContentText(getString(R.string.keep_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "keepalive"
        private const val NOTIFICATION_ID = 8686
        private val holders = HashSet<String>()

        /** Mendaftarkan pekerjaan [tag]. Layanan dimulai jika ini pekerjaan pertama. */
        @Synchronized
        fun acquire(context: Context, tag: String) {
            val first = holders.isEmpty()
            holders.add(tag)
            if (first) {
                try {
                    ContextCompat.startForegroundService(context, Intent(context, KeepAliveService::class.java))
                } catch (e: Exception) {
                    // Android menolak jika aplikasi tidak sedang tampil. Pekerjaan tetap jalan selama aplikasi terbuka.
                }
            }
        }

        /** Melepas pekerjaan [tag]. Layanan berhenti jika tidak ada pekerjaan lain. */
        @Synchronized
        fun release(context: Context, tag: String) {
            holders.remove(tag)
            if (holders.isEmpty()) context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}

/** Keterangan satu server yang sedang berjalan, untuk ditampilkan di dialognya. */
data class ServerInfo(val port: Int, val token: String = "", val user: String = "", val password: String = "")

data class NetStatus(
    val ftp: ServerInfo? = null,
    val http: ServerInfo? = null,
    val lan: ServerInfo? = null,
    val error: String? = null,
)

/** Tiga server berbagi berkas: FTP, HTTP, dan Transfer LAN. Hidup selama dinyalakan dari dialognya. */
object NetServers {

    const val FTP_PORT = 2121
    const val HTTP_PORT = 8080
    const val LAN_PORT = 8686
    const val FTP_USER = "filemate"

    /** Folder tempat berkas kiriman Transfer LAN disimpan. */
    val receiveDir: File get() = File(FileRepository.FILEMATE_DIR, "Terima")

    private val status = MutableStateFlow(NetStatus())
    val state: StateFlow<NetStatus> = status.asStateFlow()

    private var ftp: FtpServer? = null
    private var http: HttpFileServer? = null
    private var lan: HttpFileServer? = null

    /** Kode acak yang mudah diketik: huruf kecil dan angka. */
    private fun code(length: Int): String {
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    @Synchronized
    fun startFtp(context: Context) {
        attempt {
            if (ftp != null) return@attempt
            val password = code(6)
            val server = FtpServer(File(FileRepository.STORAGE_ROOT), FTP_PORT, FTP_USER, password)
            server.start()
            ftp = server
            KeepAliveService.acquire(context, "ftp")
            status.update { it.copy(ftp = ServerInfo(FTP_PORT, user = FTP_USER, password = password), error = null) }
        }
    }

    @Synchronized
    fun stopFtp(context: Context) {
        ftp?.stop()
        ftp = null
        KeepAliveService.release(context, "ftp")
        status.update { it.copy(ftp = null) }
    }

    @Synchronized
    fun startHttp(context: Context, title: String) {
        attempt {
            if (http != null) return@attempt
            val token = code(8)
            val server = HttpFileServer(File(FileRepository.STORAGE_ROOT), HTTP_PORT, token, null, title)
            server.start()
            http = server
            KeepAliveService.acquire(context, "http")
            status.update { it.copy(http = ServerInfo(HTTP_PORT, token = token), error = null) }
        }
    }

    @Synchronized
    fun stopHttp(context: Context) {
        http?.stop()
        http = null
        KeepAliveService.release(context, "http")
        status.update { it.copy(http = null) }
    }

    @Synchronized
    fun startLan(context: Context, title: String) {
        attempt {
            if (lan != null) return@attempt
            val token = code(8)
            receiveDir.mkdirs()
            // Halaman Transfer LAN hanya menampilkan folder kiriman, bukan seluruh penyimpanan
            val server = HttpFileServer(receiveDir, LAN_PORT, token, receiveDir, title)
            server.start()
            lan = server
            KeepAliveService.acquire(context, "lan")
            status.update { it.copy(lan = ServerInfo(LAN_PORT, token = token), error = null) }
        }
    }

    @Synchronized
    fun stopLan(context: Context) {
        lan?.stop()
        lan = null
        KeepAliveService.release(context, "lan")
        status.update { it.copy(lan = null) }
    }

    /** Menjalankan [block]. Kegagalan (misalnya port sedang dipakai) dicatat supaya bisa ditampilkan. */
    private fun attempt(block: () -> Unit) {
        // Membuka port dilakukan di luar thread utama, karena Android melarang kerja jaringan di sana
        Thread {
            synchronized(this) {
                try {
                    block()
                } catch (e: Exception) {
                    status.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
                }
            }
        }.start()
    }
}
