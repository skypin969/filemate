package com.filemate.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.filemate.app.ui.FilemateApp
import com.filemate.app.ui.FilemateTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
}
