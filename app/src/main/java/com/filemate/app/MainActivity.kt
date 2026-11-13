package com.filemate.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.filemate.app.ui.AppPrefs
import com.filemate.app.ui.FilemateApp
import com.filemate.app.ui.FilemateTheme
import com.filemate.app.ui.PendingOpen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.applyStartup(this)
        readShortcut(intent)
        // Ikon bilah status dibuat putih, karena bilah atas aplikasi berwarna
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        // Bahasa awal: Bahasa Indonesia. Jika user sudah memilih English, pilihan itu dipakai.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("id"))
        }

        setContent {
            FilemateTheme {
                FilemateApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readShortcut(intent)
    }

    /** Pintasan folder di layar utama HP membawa alamat folder yang harus dibuka. */
    private fun readShortcut(intent: Intent?) {
        val path = intent?.getStringExtra(PendingOpen.EXTRA_PATH)
        if (!path.isNullOrEmpty()) PendingOpen.path = path
    }
}
