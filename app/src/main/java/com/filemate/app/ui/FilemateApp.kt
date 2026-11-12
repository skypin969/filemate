package com.filemate.app.ui

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Layar-layar aplikasi, berurutan dari awal sampai akhir. */
enum class Screen { SPLASH, PERMISSION, MAIN, SETTINGS }

/** Pengatur layar: memilih layar mana yang tampil. */
@Composable
fun FilemateApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }

    when (screen) {
        Screen.SPLASH -> SplashScreen(
            onFinished = {
                screen = if (Environment.isExternalStorageManager()) Screen.MAIN else Screen.PERMISSION
            },
        )

        Screen.PERMISSION -> PermissionScreen(
            onGranted = { screen = Screen.MAIN },
        )

        Screen.MAIN -> MainScreen(
            onOpenSettings = { screen = Screen.SETTINGS },
        )

        Screen.SETTINGS -> {
            BackHandler { screen = Screen.MAIN }
            SettingsScreen(onBack = { screen = Screen.MAIN })
        }
    }
}
