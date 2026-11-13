package com.filemate.app.ui

import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SAMPLE_SIZES = listOf(12, 18, 24, 36, 48, 60)

/**
 * Penampil font seperti MT Manager: contoh huruf kecil, huruf besar, angka, dan tanda baca,
 * lalu satu kalimat contoh dalam beberapa ukuran. Tombol pensil mengganti kalimat contohnya.
 */
@Composable
fun FontPreviewScreen(path: String, onBack: () -> Unit) {
    val file = remember(path) { File(path) }
    // null = sedang dimuat, Result gagal = berkas bukan font yang bisa dibaca
    val loaded by produceState<Result<FontFamily>?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                Result.success(FontFamily(Typeface.createFromFile(file)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    var sample by remember { mutableStateOf("Filemate") }
    var editOpen by remember { mutableStateOf(false) }
    var infoOpen by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBrush)
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.open_font),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = file.name,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { infoOpen = true }) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.menu_attributes), tint = Color.White)
            }
            IconButton(onClick = { editOpen = true }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.font_sample_title), tint = Color.White)
            }
        }

        val result = loaded
        val family = result?.getOrNull()
        if (result != null && family == null) {
            Text(
                text = stringResource(R.string.font_failed),
                color = Danger,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
        if (family != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                listOf(
                    "abcdefghijklmnopqrstuvwxyz",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                    "0123456789",
                    ". : , ; ' \" ! ? + - * / % = ~ |",
                    "@ # $ _ ^ \\ & < > ( ) [ ] { }",
                ).forEach { line ->
                    Text(text = line, color = Ink, fontFamily = family, fontSize = 20.sp, lineHeight = 28.sp)
                }
                SAMPLE_SIZES.forEach { size ->
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(text = size.toString(), color = Muted, fontSize = 10.sp, modifier = Modifier.width(22.dp))
                        Text(
                            text = sample,
                            color = Ink,
                            fontFamily = family,
                            fontSize = size.sp,
                            lineHeight = (size * 1.25f).sp,
                        )
                    }
                }
            }
        }
    }

    if (editOpen) {
        InputDialog(
            title = stringResource(R.string.font_sample_title),
            label = stringResource(R.string.font_sample_hint),
            initial = sample,
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = {
                sample = it
                editOpen = false
            },
            onDismiss = { editOpen = false },
        )
    }

    if (infoOpen) {
        AlertDialog(
            onDismissRequest = { infoOpen = false },
            containerColor = Color.White,
            title = { Text(file.name, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    text = file.parent.orEmpty() + "\n" + formatSize(file.length()) + "\n" + formatDateFull(file.lastModified()),
                    color = Ink,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { infoOpen = false }) {
                    Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}
