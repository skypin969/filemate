package com.filemate.app.ui

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Layar-layar aplikasi, berurutan dari awal sampai akhir. */
enum class Screen { SPLASH, PERMISSION, MAIN, SETTINGS, APPS, TRASH }

/** Pengatur layar: memilih layar mana yang tampil. */
@Composable
fun FilemateApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    // Editor teks atau hex tampil menutupi layar utama. Tidak ikut tersimpan jika aplikasi ditutup paksa.
    var editor by remember { mutableStateOf<EditorTarget?>(null) }

    // Terminal juga tampil menutupi layar utama
    var terminal by remember { mutableStateOf<TerminalTarget?>(null) }

    val open = editor
    if (open != null) {
        EditorScreen(target = open, onBack = { editor = null })
        return
    }

    // Alat dari menu samping yang tampil sebagai layar penuh. Isi kedua adalah berkas yang dibuka (jika ada).
    var tool by remember { mutableStateOf<Pair<ToolKind, String?>?>(null) }

    // Pratinjau berkas HTML atau Markdown
    var preview by remember { mutableStateOf<PreviewTarget?>(null) }

    val shell = terminal
    if (shell != null) {
        TerminalScreen(target = shell, onBack = { terminal = null })
        return
    }

    val shown = preview
    if (shown != null) {
        PreviewScreen(
            target = shown,
            onBack = { preview = null },
            onEdit = {
                preview = null
                editor = EditorTarget(shown.path, hex = false, root = false).also { EditorSession.open(it) }
            },
        )
        return
    }

    val openTool = tool
    if (openTool != null) {
        val close = { tool = null }
        when (openTool.first) {
            ToolKind.TOOLBOX -> ToolboxScreen(close)
            ToolKind.TIMESTAMP -> TimestampScreen(close)
            ToolKind.SMALI -> SmaliQueryScreen(close)
            ToolKind.RULER -> RulerScreen(close)
            ToolKind.NETWORK -> NetworkToolsScreen(close)
            ToolKind.DECIBEL -> DecibelScreen(close)
            ToolKind.LAYOUT -> LayoutViewerScreen(close)
            ToolKind.COLOR -> ColorPickerScreen(close)
            ToolKind.CLASSES -> ClassAnalysisScreen(openTool.second.orEmpty(), close)
            ToolKind.FONT -> FontPreviewScreen(openTool.second.orEmpty(), close)
            ToolKind.KEYS -> KeyManagerScreen(close)
            ToolKind.PASSWORDS -> PasswordsScreen(close)
            // FTP, HTTP, dan Transfer LAN berupa dialog di layar utama, bukan layar penuh
            else -> tool = null
        }
        return
    }

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
            onOpenEditor = {
                // Editor teks menyimpan daftar berkas yang terbuka, editor hex tidak
                if (!it.hex) EditorSession.open(it)
                editor = it
            },
            onOpenTerminal = { terminal = it },
            onOpenApps = { screen = Screen.APPS },
            onOpenTrash = { screen = Screen.TRASH },
            onOpenTool = { kind, path -> tool = Pair(kind, path) },
            onOpenPreview = { preview = it },
        )

        Screen.TRASH -> TrashScreen(onBack = { screen = Screen.MAIN })

        Screen.APPS -> AppManagerScreen(onBack = { screen = Screen.MAIN })

        Screen.SETTINGS -> {
            BackHandler { screen = Screen.MAIN }
            SettingsScreen(onBack = { screen = Screen.MAIN })
        }
    }
}
