package com.filemate.app.ui

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
