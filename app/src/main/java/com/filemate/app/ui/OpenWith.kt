package com.filemate.app.ui

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
    TEXT, ARCHIVE, IMAGE, HEX, MUSIC, XML, DEX, ARSC, APK_INFO, APKS, SCRIPT, FONT,
    SIGNATURE, SVG, JAR, PDF, CLASS, MARKDOWN, DATABASE, NINE_PATCH,
}

private class OpenChoice(
    val mode: OpenMode,
    val label: Int,
    val icon: ImageVector,
    val tint: Color,
)

// Urutan dan warna mengikuti dialog "Buka dengan" di NP Manager
private val CHOICES = listOf(
    OpenChoice(OpenMode.TEXT, R.string.open_text, Icons.Filled.Description, Color(0xFF3F51B5)),
    OpenChoice(OpenMode.ARCHIVE, R.string.open_archive, Icons.Filled.Archive, Color(0xFFD32F2F)),
    OpenChoice(OpenMode.IMAGE, R.string.open_image, Icons.Filled.Image, Color(0xFF2EAD6B)),
    OpenChoice(OpenMode.HEX, R.string.open_hex, Icons.Filled.DataObject, Color(0xFF546E7A)),
    OpenChoice(OpenMode.MUSIC, R.string.open_music, Icons.Filled.MusicNote, Color(0xFF7E57C2)),
    OpenChoice(OpenMode.XML, R.string.open_xml, Icons.Filled.Code, Color(0xFF3F6FD8)),
    OpenChoice(OpenMode.DEX, R.string.open_dex, Icons.Filled.Widgets, Color(0xFF2E8B8B)),
    OpenChoice(OpenMode.ARSC, R.string.open_arsc, Icons.Filled.GridOn, Color(0xFFE8A33A)),
    OpenChoice(OpenMode.APK_INFO, R.string.open_apk_info, Icons.Filled.Android, Color(0xFF3DDC84)),
    OpenChoice(OpenMode.APKS, R.string.open_apks, Icons.Filled.Adb, Color(0xFFC0392B)),
    OpenChoice(OpenMode.SCRIPT, R.string.open_script, Icons.Filled.Terminal, Color(0xFF111111)),
    OpenChoice(OpenMode.FONT, R.string.open_font, Icons.Filled.TextFields, Color(0xFF3F6FD8)),
    OpenChoice(OpenMode.SIGNATURE, R.string.open_signature, Icons.Filled.VpnKey, Color(0xFF7CB342)),
    OpenChoice(OpenMode.SVG, R.string.open_svg, Icons.Filled.Photo, Color(0xFF2EAD6B)),
    OpenChoice(OpenMode.JAR, R.string.open_jar, Icons.Filled.Coffee, Color(0xFFC0392B)),
    OpenChoice(OpenMode.PDF, R.string.open_pdf, Icons.Filled.PictureAsPdf, Color(0xFFE53935)),
    OpenChoice(OpenMode.CLASS, R.string.open_class, Icons.Filled.Article, Color(0xFF7E57C2)),
    OpenChoice(OpenMode.MARKDOWN, R.string.open_markdown, Icons.Filled.Tag, Color(0xFF455A64)),
    OpenChoice(OpenMode.DATABASE, R.string.open_database, Icons.Filled.Storage, Color(0xFF9E9E9E)),
    OpenChoice(OpenMode.NINE_PATCH, R.string.open_nine_patch, Icons.Filled.Photo, Color(0xFF26A69A)),
)

/**
 * Dialog "Buka dengan..." seperti NP Manager: kotak ikon berwarna tiga kolom.
 * Tombol "Saran sistem" membuka pilihan aplikasi bawaan Android.
 */
@Composable
fun OpenWithDialog(
    onPick: (OpenMode) -> Unit,
    onSystem: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) {
                Text(
                    text = stringResource(R.string.open_title),
                    color = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    CHOICES.chunked(3).forEach { rowChoices ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                            rowChoices.forEach { choice ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onPick(choice.mode) }
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = choice.icon,
                                        contentDescription = null,
                                        tint = choice.tint,
                                        modifier = Modifier.size(38.dp),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(choice.label),
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
                    TextButton(onClick = onSystem) {
                        Text(stringResource(R.string.open_system), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
