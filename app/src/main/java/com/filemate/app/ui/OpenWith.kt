package com.filemate.app.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.filemate.app.R

/** Cara membuka berkas dari dialog "Buka dengan". */
enum class OpenMode {
    TEXT, ARCHIVE, IMAGE, HEX, MUSIC, VIDEO, XML, DEX, ARSC, APK_INFO, APKS, SCRIPT, FONT,
    SIGNATURE, SVG, JAR, PDF, CLASS, MARKDOWN, DATABASE, NINE_PATCH,
}

private class OpenChoice(
    val mode: OpenMode,
    val label: Int,
    val icon: ImageVector,
)

// Urutan mengikuti dialog "Buka dengan" di NP Manager. Ikon satu warna, sesuai tema hitam putih.
private val CHOICES = listOf(
    OpenChoice(OpenMode.TEXT, R.string.open_text, Icons.Filled.Description),
    OpenChoice(OpenMode.ARCHIVE, R.string.open_archive, Icons.Filled.Archive),
    OpenChoice(OpenMode.IMAGE, R.string.open_image, Icons.Filled.Image),
    OpenChoice(OpenMode.HEX, R.string.open_hex, Icons.Filled.DataObject),
    OpenChoice(OpenMode.MUSIC, R.string.open_music, Icons.Filled.MusicNote),
    OpenChoice(OpenMode.VIDEO, R.string.open_video, Icons.Filled.Movie),
    OpenChoice(OpenMode.XML, R.string.open_xml, Icons.Filled.Code),
    OpenChoice(OpenMode.DEX, R.string.open_dex, Icons.Filled.Widgets),
    OpenChoice(OpenMode.ARSC, R.string.open_arsc, Icons.Filled.GridOn),
    OpenChoice(OpenMode.APK_INFO, R.string.open_apk_info, Icons.Filled.Android),
    OpenChoice(OpenMode.APKS, R.string.open_apks, Icons.Filled.Adb),
    OpenChoice(OpenMode.SCRIPT, R.string.open_script, Icons.Filled.Terminal),
    OpenChoice(OpenMode.FONT, R.string.open_font, Icons.Filled.TextFields),
    OpenChoice(OpenMode.SIGNATURE, R.string.open_signature, Icons.Filled.VpnKey),
    OpenChoice(OpenMode.SVG, R.string.open_svg, Icons.Filled.Photo),
    OpenChoice(OpenMode.JAR, R.string.open_jar, Icons.Filled.Coffee),
    OpenChoice(OpenMode.PDF, R.string.open_pdf, Icons.Filled.PictureAsPdf),
    OpenChoice(OpenMode.CLASS, R.string.open_class, Icons.Filled.Article),
    OpenChoice(OpenMode.MARKDOWN, R.string.open_markdown, Icons.Filled.Tag),
    OpenChoice(OpenMode.DATABASE, R.string.open_database, Icons.Filled.Storage),
    OpenChoice(OpenMode.NINE_PATCH, R.string.open_nine_patch, Icons.Filled.Photo),
)

/** Cara buka bawaan per akhiran berkas, disimpan di pengaturan. Diatur dengan menekan lama ikon di dialog "Buka dengan". */
object OpenDefaults {
    private const val PREFIX = "open_default_"

    fun get(context: Context, extension: String): OpenMode? {
        if (extension.isEmpty()) return null
        val name = AppPrefs.of(context).getString(PREFIX + extension.uppercase(), null) ?: return null
        return OpenMode.entries.firstOrNull { it.name == name }
    }

    fun set(context: Context, extension: String, mode: OpenMode) {
        if (extension.isEmpty()) return
        AppPrefs.of(context).edit().putString(PREFIX + extension.uppercase(), mode.name).apply()
    }

    fun remove(context: Context, extension: String) {
        AppPrefs.of(context).edit().remove(PREFIX + extension.uppercase()).apply()
    }

    /** Semua cara buka bawaan yang tersimpan: akhiran berkas dan caranya, urut abjad. */
    fun all(context: Context): List<Pair<String, OpenMode>> =
        AppPrefs.of(context).all.entries
            .filter { it.key.startsWith(PREFIX) }
            .mapNotNull { entry ->
                val mode = OpenMode.entries.firstOrNull { it.name == entry.value } ?: return@mapNotNull null
                entry.key.removePrefix(PREFIX) to mode
            }
            .sortedBy { it.first }

    /** Nama cara buka untuk ditampilkan. */
    fun labelOf(mode: OpenMode): Int = CHOICES.firstOrNull { it.mode == mode }?.label ?: R.string.open_title
}

/**
 * Dialog "Buka dengan..." seperti MT Manager: kotak ikon tiga kolom, tombol cari dan bantuan di atas,
 * serta tombol "Jenis" untuk membuka lewat aplikasi lain menurut jenisnya (teks, gambar, video, audio, semua).
 * Menekan lama satu ikon menjadikannya cara buka bawaan untuk akhiran berkas [extension].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenWithDialog(
    extension: String,
    onPick: (OpenMode) -> Unit,
    onMakeDefault: (OpenMode) -> Unit,
    onSystem: () -> Unit,
    onSystemAs: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var typeMenu by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    var manageOpen by remember { mutableStateOf(false) }
    // Naik setiap ada cara buka bawaan yang dihapus, supaya daftarnya dibaca ulang
    var defaultsVersion by remember { mutableStateOf(0) }
    val labeled = CHOICES.map { it to stringResource(it.label) }
    val shown = labeled.filter { query.isBlank() || it.second.contains(query.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (searching) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = Ink),
                            cursorBrush = SolidColor(Accent),
                            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        )
                        IconButton(onClick = {
                            searching = false
                            query = ""
                        }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.cd_close), tint = Muted)
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.open_title),
                            color = Ink,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Filled.Search, stringResource(R.string.menu_search), tint = Muted)
                        }
                        IconButton(onClick = { helpOpen = true }) {
                            Icon(Icons.Filled.HelpOutline, stringResource(R.string.open_help_title), tint = Muted)
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    shown.chunked(3).forEach { rowChoices ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                            rowChoices.forEach { (choice, label) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            onClick = { onPick(choice.mode) },
                                            onLongClick = { onMakeDefault(choice.mode) },
                                        )
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = choice.icon,
                                        contentDescription = null,
                                        tint = MenuIcon,
                                        modifier = Modifier.size(38.dp),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = label,
                                        color = Muted,
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                    )
                                }
                            }
                            repeat(3 - rowChoices.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box {
                        TextButton(onClick = { typeMenu = true }) {
                            Text(stringResource(R.string.open_type), color = Accent, fontWeight = FontWeight.SemiBold)
                        }
                        // Membuka lewat aplikasi lain di HP, menurut jenis yang dipilih
                        DropdownMenu(
                            expanded = typeMenu,
                            onDismissRequest = { typeMenu = false },
                            containerColor = Color.White,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.open_system), color = Ink, fontSize = 15.sp) },
                                onClick = {
                                    typeMenu = false
                                    onSystem()
                                },
                            )
                            listOf(
                                R.string.open_type_text to "text/plain",
                                R.string.open_type_image to "image/*",
                                R.string.open_type_video to "video/*",
                                R.string.open_type_audio to "audio/*",
                                R.string.open_type_all to "*/*",
                            ).forEach { (label, mime) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(label), color = Ink, fontSize = 15.sp) },
                                    onClick = {
                                        typeMenu = false
                                        onSystemAs(mime)
                                    },
                                )
                            }
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (helpOpen) {
        AlertDialog(
            onDismissRequest = { helpOpen = false },
            containerColor = Color.White,
            title = { Text(stringResource(R.string.open_help_title), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.open_help_body), color = Ink, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { helpOpen = false }) {
                    Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    helpOpen = false
                    manageOpen = true
                }) {
                    Text(stringResource(R.string.open_manage), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    if (manageOpen) {
        val defaults = remember(defaultsVersion) { OpenDefaults.all(context) }
        AlertDialog(
            onDismissRequest = { manageOpen = false },
            containerColor = Color.White,
            title = { Text(stringResource(R.string.open_defaults_title), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (defaults.isEmpty()) {
                        Text(stringResource(R.string.open_defaults_empty), color = Muted, fontSize = 14.sp)
                    }
                    defaults.forEach { (ext, mode) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "." + ext.lowercase() + "  →  " + stringResource(OpenDefaults.labelOf(mode)),
                                color = Ink,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                OpenDefaults.remove(context, ext)
                                defaultsVersion++
                            }) {
                                Icon(Icons.Filled.Close, stringResource(R.string.btn_delete), tint = Muted)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { manageOpen = false }) {
                    Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}
