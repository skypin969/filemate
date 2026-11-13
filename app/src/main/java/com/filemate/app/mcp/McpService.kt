package com.filemate.app.mcp

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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.filemate.app.MainActivity
import com.filemate.app.R
import com.filemate.app.ui.AppPrefs
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Keadaan layanan MCP yang ditampilkan di dialog APK MCP. */
data class McpStatus(
    val running: Boolean = false,
    val port: Int = 0,
    val lan: Boolean = false,
    /** Pesan kesalahan saat layanan gagal dimulai, misalnya port sedang dipakai. */
    val error: String? = null,
    /** Catatan pemanggilan alat, yang terbaru di atas. */
    val history: List<String> = emptyList(),
)

/**
 * Layanan latar depan yang menjaga server MCP tetap hidup walau Filemate tidak sedang dibuka.
 * Selama layanan berjalan, ada notifikasi tetap di bilah status.
 */
class McpService : Service() {

    @Volatile
    private var server: McpServer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var starting = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppPrefs.applyStartup(this)
        val prefs = AppPrefs.of(this)
        val port = prefs.getInt(AppPrefs.KEY_MCP_PORT, DEFAULT_PORT)
        val lan = prefs.getBoolean(AppPrefs.KEY_MCP_LAN, false)

        // Android mewajibkan notifikasi segera ditampilkan begitu layanan latar depan dimulai
        val notification = buildNotification(port)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (server == null && !starting) {
            starting = true
            // Membuka port dilakukan di luar thread utama, karena Android melarang kerja jaringan di sana
            Thread {
                try {
                    val created = McpServer(applicationContext, port, lan) { tool, millis, ok ->
                        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                        val line = "$time  $tool  ${millis}ms  ${if (ok) "OK" else "GAGAL"}"
                        status.update { it.copy(history = (listOf(line) + it.history).take(200)) }
                    }
                    created.start()
                    server = created
                    // Menjaga prosesor tetap bekerja saat layar mati, supaya sambungan AI tidak putus
                    val power = getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "filemate:mcp").also { it.acquire() }
                    status.update { it.copy(running = true, port = port, lan = lan, error = null) }
                } catch (e: Exception) {
                    status.update { it.copy(running = false, error = e.message ?: e.javaClass.simpleName) }
                    stopSelf()
                } finally {
                    starting = false
                }
            }.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        status.update { it.copy(running = false) }
        super.onDestroy()
    }

    private fun buildNotification(port: Int): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.mcp_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.mcp_notification_title))
            .setContentText(getString(R.string.mcp_notification_text, port))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val DEFAULT_PORT = 8787
        private const val CHANNEL_ID = "mcp"
        private const val NOTIFICATION_ID = 8787

        private val status = MutableStateFlow(McpStatus())

        /** Keadaan layanan, diamati oleh dialog APK MCP. */
        val state: StateFlow<McpStatus> = status.asStateFlow()

        fun start(context: Context) {
            status.update { it.copy(error = null) }
            ContextCompat.startForegroundService(context, Intent(context, McpService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, McpService::class.java))
        }

        /** Alamat IPv4 HP di jaringan Wi-Fi atau hotspot. Null jika tidak tersambung ke jaringan lokal. */
        fun lanAddress(): String? = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
