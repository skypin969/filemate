package com.filemate.app.ui

import android.widget.Toast
import com.filemate.app.apk.Jar2Dex
import com.filemate.app.data.FileRepository
import com.filemate.app.data.RootShell
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.filemate.app.R

/**
 * Layar pengaturan, susunannya mengikuti NP Manager: judul bagian berwarna biru,
 * lalu baris berisi judul, keterangan abu-abu, dan sakelar di kanan.
 * Hanya pengaturan yang benar-benar berfungsi yang ditampilkan.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs.of(context) }
    val languageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isEnglish = languageTag.startsWith("en")
    val isAuto = languageTag.isEmpty()
    var listSize by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_LIST_SIZE, 1)) }
    var nameLines by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_NAME_LINES, 3)) }
    var bottomMargin by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_BOTTOM_MARGIN, 0)) }
    var thumbs by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_THUMBS, true)) }
    // Dialog pilihan yang sedang terbuka: "size", "lines", atau "margin"
    var choice by remember { mutableStateOf<String?>(null) }
    val sizeLabels = listOf(
        stringResource(R.string.settings_size_small),
        stringResource(R.string.settings_size_medium),
        stringResource(R.string.settings_size_large),
    )
    var backup by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_BACKUP, false)) }
    var home by remember { mutableStateOf(prefs.getString(AppPrefs.KEY_HOME, "").orEmpty()) }
    var showAbout by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(ThemeColors.primary) }
    var suCommand by remember { mutableStateOf(prefs.getString(AppPrefs.KEY_SU, "su").orEmpty().ifBlank { "su" }) }
    var workDir by remember { mutableStateOf(FileRepository.FILEMATE_DIR) }
    var thumbMb by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_THUMB_MAX_MB, 10)) }
    var thumbTime by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_THUMB_TIMEOUT, 10)) }
    var installer by remember { mutableStateOf(prefs.getString(AppPrefs.KEY_INSTALLER, "").orEmpty()) }
    // Kolom isian yang sedang terbuka: "su", "dir", atau "installer"
    var input by remember { mutableStateOf<String?>(null) }
    val invalidDir = stringResource(R.string.settings_dir_invalid)
    var jarMinSdk by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_JAR_MIN_SDK, Jar2Dex.DEFAULT_MIN_SDK)) }

    /** Sakelar sederhana yang nilainya langsung disimpan. */
    @Composable
    fun Toggle(key: String, default: Boolean, title: Int, desc: Int?) {
        var on by remember { mutableStateOf(prefs.getBoolean(key, default)) }
        SettingRow(
            title = stringResource(title),
            subtitle = desc?.let { stringResource(it) },
            checked = on,
            onClick = {
                on = !on
                prefs.edit().putBoolean(key, on).apply()
            },
        )
    }
    var showLanguage by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(PageGray)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBrush)
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            SectionTitle(stringResource(R.string.settings_start_header))
            SettingRow(
                title = stringResource(R.string.settings_su_title),
                subtitle = suCommand,
                onClick = { input = "su" },
            )
            SettingRow(
                title = stringResource(R.string.settings_home_title),
                subtitle = if (home.isEmpty()) {
                    stringResource(R.string.title_storage)
                } else {
                    home + "\n" + stringResource(R.string.settings_home_reset)
                },
                onClick = {
                    home = ""
                    prefs.edit().remove(AppPrefs.KEY_HOME).apply()
                },
            )

            SectionTitle(stringResource(R.string.settings_display_header))
            SettingRow(
                title = stringResource(R.string.settings_theme_title),
                subtitle = stringResource(R.string.settings_theme_desc),
                onClick = { choice = "theme" },
            )
            SettingRow(
                title = stringResource(R.string.settings_lang_header),
                subtitle = stringResource(
                    when {
                        isAuto -> R.string.settings_lang_auto
                        isEnglish -> R.string.settings_lang_en
                        else -> R.string.settings_lang_id
                    },
                ),
                onClick = { showLanguage = true },
            )
            SettingRow(
                title = stringResource(R.string.settings_list_size),
                subtitle = sizeLabels[listSize.coerceIn(0, 2)],
                onClick = { choice = "size" },
            )
            SettingRow(
                title = stringResource(R.string.settings_name_lines),
                subtitle = nameLines.toString(),
                onClick = { choice = "lines" },
            )
            SettingRow(
                title = stringResource(R.string.settings_bottom_margin),
                subtitle = "$bottomMargin dp",
                onClick = { choice = "margin" },
            )
            SettingRow(
                title = stringResource(R.string.settings_thumbs_title),
                subtitle = stringResource(R.string.settings_thumbs_desc),
                checked = thumbs,
                onClick = {
                    thumbs = !thumbs
                    prefs.edit().putBoolean(AppPrefs.KEY_THUMBS, thumbs).apply()
                },
            )
            SettingRow(
                title = stringResource(R.string.settings_thumb_max_title),
                subtitle = stringResource(R.string.settings_thumb_max_desc, thumbMb),
                onClick = { choice = "thumbmb" },
            )
            SettingRow(
                title = stringResource(R.string.settings_thumb_time_title),
                subtitle = stringResource(R.string.settings_thumb_time_desc, thumbTime),
                onClick = { choice = "thumbtime" },
            )

            SectionTitle(stringResource(R.string.settings_editor_header))
            Toggle(AppPrefs.KEY_MD_PREVIEW, true, R.string.settings_md_preview, null)
            Toggle(AppPrefs.KEY_HTML_PREVIEW, true, R.string.settings_html_preview, null)
            Toggle(AppPrefs.KEY_FLUID, false, R.string.settings_fluid_title, R.string.settings_fluid_desc)

            SectionTitle(stringResource(R.string.settings_general_header))
            SettingRow(
                title = stringResource(R.string.settings_backup_title),
                subtitle = stringResource(R.string.settings_backup_desc),
                checked = backup,
                onClick = {
                    backup = !backup
                    prefs.edit().putBoolean(AppPrefs.KEY_BACKUP, backup).apply()
                },
            )
            SettingRow(
                title = stringResource(R.string.settings_dir_title),
                subtitle = workDir,
                onClick = { input = "dir" },
            )

            SectionTitle(stringResource(R.string.tool_recycle))
            Toggle(AppPrefs.KEY_TRASH_ON, true, R.string.settings_trash_on_title, R.string.settings_trash_on_desc)
            Toggle(AppPrefs.KEY_TRASH_DEFAULT, true, R.string.settings_trash_default_title, R.string.settings_trash_default_desc)
            Toggle(AppPrefs.KEY_DELETE_WARNING, true, R.string.settings_warning_title, R.string.settings_warning_desc)

            SectionTitle(stringResource(R.string.settings_apk_header))
            Toggle(AppPrefs.KEY_AUTO_SIGN, true, R.string.settings_auto_sign, null)
            Toggle(AppPrefs.KEY_ROOT_INSTALL, true, R.string.settings_root_install_title, R.string.settings_root_install_desc)
            SettingRow(
                title = stringResource(R.string.settings_installer_title),
                subtitle = installer.ifEmpty { stringResource(R.string.settings_default) },
                onClick = { input = "installer" },
            )
            SettingRow(
                title = stringResource(R.string.settings_jar_title),
                subtitle = stringResource(R.string.settings_jar_desc, jarMinSdk),
                onClick = { input = "jar" },
            )

            SectionTitle(stringResource(R.string.settings_other_header))
            SettingRow(
                title = stringResource(R.string.settings_about),
                subtitle = null,
                onClick = { showAbout = true },
            )
        }
    }

    if (showLanguage) {
        AlertDialog(
            onDismissRequest = { showLanguage = false },
            containerColor = Color.White,
            title = { Text(stringResource(R.string.settings_lang_header), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    LanguageRow(stringResource(R.string.settings_lang_auto), isAuto) {
                        showLanguage = false
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                    }
                    LanguageRow(stringResource(R.string.settings_lang_id), !isEnglish && !isAuto) {
                        showLanguage = false
                        setLanguage("id")
                    }
                    LanguageRow(stringResource(R.string.settings_lang_en), isEnglish) {
                        showLanguage = false
                        setLanguage("en")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguage = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Accent)
                }
            },
        )
    }

    when (choice) {
        "size" -> ChoiceDialog(
            title = stringResource(R.string.settings_list_size),
            options = sizeLabels.mapIndexed { index, label -> label to index },
            current = listSize,
            onPick = {
                listSize = it
                prefs.edit().putInt(AppPrefs.KEY_LIST_SIZE, it).apply()
                choice = null
            },
            onDismiss = { choice = null },
        )

        "lines" -> ChoiceDialog(
            title = stringResource(R.string.settings_name_lines),
            options = (1..8).map { it.toString() to it },
            current = nameLines,
            onPick = {
                nameLines = it
                prefs.edit().putInt(AppPrefs.KEY_NAME_LINES, it).apply()
                choice = null
            },
            onDismiss = { choice = null },
        )

        "thumbmb" -> ChoiceDialog(
            title = stringResource(R.string.settings_thumb_max_title),
            options = listOf(1, 5, 10, 20, 50, 100).map { "$it MB" to it },
            current = thumbMb,
            onPick = {
                thumbMb = it
                prefs.edit().putInt(AppPrefs.KEY_THUMB_MAX_MB, it).apply()
                choice = null
            },
            onDismiss = { choice = null },
        )

        "thumbtime" -> ChoiceDialog(
            title = stringResource(R.string.settings_thumb_time_title),
            options = listOf(3, 5, 10, 20, 30, 60).map { it.toString() to it },
            current = thumbTime,
            onPick = {
                thumbTime = it
                prefs.edit().putInt(AppPrefs.KEY_THUMB_TIMEOUT, it).apply()
                choice = null
            },
            onDismiss = { choice = null },
        )

        "theme" -> ThemeDialog(
            current = theme,
            onPick = {
                theme = it
                ThemeColors.primary = it
                prefs.edit().putInt(AppPrefs.KEY_THEME, it).apply()
                choice = null
            },
            onDismiss = { choice = null },
        )

        "margin" -> ChoiceDialog(
            title = stringResource(R.string.settings_bottom_margin),
            options = listOf(0, 8, 16, 24, 32, 48).map { "$it dp" to it },
            current = bottomMargin,
            onPick = {
                bottomMargin = it
                prefs.edit().putInt(AppPrefs.KEY_BOTTOM_MARGIN, it).apply()
                choice = null
            },
            onDismiss = { choice = null },
        )
    }

    when (input) {
        "su" -> InputDialog(
            title = stringResource(R.string.settings_su_title),
            label = "su",
            initial = suCommand,
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                suCommand = typed.trim().ifBlank { "su" }
                prefs.edit().putString(AppPrefs.KEY_SU, suCommand).apply()
                RootShell.suCommand = suCommand
                input = null
            },
            onDismiss = { input = null },
        )

        "dir" -> InputDialog(
            title = stringResource(R.string.settings_dir_title),
            label = FileRepository.DEFAULT_FILEMATE_DIR,
            initial = workDir,
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                val dir = typed.trim().trimEnd('/').ifBlank { FileRepository.DEFAULT_FILEMATE_DIR }
                // Folder kerja harus di penyimpanan internal, supaya semua alat bisa menulis ke sana
                if (dir.startsWith(FileRepository.STORAGE_ROOT + "/")) {
                    workDir = dir
                    prefs.edit().putString(AppPrefs.KEY_WORK_DIR, dir).apply()
                    FileRepository.FILEMATE_DIR = dir
                } else {
                    Toast.makeText(context, invalidDir, Toast.LENGTH_LONG).show()
                }
                input = null
            },
            onDismiss = { input = null },
        )

        "jar" -> InputDialog(
            title = stringResource(R.string.settings_jar_title),
            label = "minSdkVersion: 13-100",
            initial = jarMinSdk.toString(),
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                // Di luar 13 sampai 100 dikembalikan ke batas terdekat
                jarMinSdk = (typed.trim().toIntOrNull() ?: Jar2Dex.DEFAULT_MIN_SDK).coerceIn(13, 100)
                prefs.edit().putInt(AppPrefs.KEY_JAR_MIN_SDK, jarMinSdk).apply()
                input = null
            },
            onDismiss = { input = null },
        )

        "installer" -> InputDialog(
            title = stringResource(R.string.settings_installer_title),
            label = "com.android.vending",
            initial = installer,
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                installer = typed.trim()
                prefs.edit().putString(AppPrefs.KEY_INSTALLER, installer).apply()
                input = null
            },
            onDismiss = { input = null },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = Color.White,
            title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.about_body)) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(R.string.btn_ok), color = Accent)
                }
            },
        )
    }
}

/** Dialog Pengaturan tema seperti NP Manager: tombol bulat di kiri dan batang warna di kanan. */
@Composable
private fun ThemeDialog(
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(stringResource(R.string.settings_theme_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ThemeColors.OPTIONS.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { selected = option },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent),
                        )
                        Spacer(Modifier.width(8.dp))
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .background(Color(option)),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected) }) {
                Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Accent)
            }
        },
    )
}

/** Dialog pilihan bulat (radio), dipakai untuk ukuran daftar, jumlah baris nama, dan margin bawah. */
@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, Int>>,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (label, value) ->
                    LanguageRow(label, value == current) { onPick(value) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Accent)
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Accent,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
    )
}

/** Satu baris pengaturan. Jika [checked] diisi, sakelar tampil di kanan. */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    checked: Boolean? = null,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = MenuIcon, fontSize = 16.sp)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = subtitle, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
                }
            }
            if (checked != null) {
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = { onClick() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                )
            }
        }
        HorizontalDivider(color = LineColor, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = Accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = Ink, fontSize = 16.sp)
    }
}

private fun setLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
