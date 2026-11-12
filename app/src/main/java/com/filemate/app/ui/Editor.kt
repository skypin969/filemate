package com.filemate.app.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import com.filemate.app.data.FileRepository
import com.filemate.app.data.RootShell
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Berkas yang dibuka di editor. [hex] memilih editor hex. [root] dipakai jika berkas perlu akses root. */
data class EditorTarget(val path: String, val hex: Boolean, val root: Boolean)

/** Batas ukuran untuk editor teks. Berkas yang lebih besar harus lewat editor hex. */
private const val MAX_TEXT_BYTES = 2L * 1024 * 1024
private const val PAGE_SIZE = 256
private const val ROW_SIZE = 8
private const val HEX_CHARS = "0123456789abcdefABCDEF"

/**
 * Berkas kerja tempat perubahan disimpan. Jika berkas asli tidak bisa diubah langsung,
 * dibuat salinan di cache lewat root, dan salinan itu dikembalikan ke aslinya saat disimpan.
 */
private class Workfile(val file: File, val isCopy: Boolean) {
    fun discard() {
        if (isCopy) file.delete()
    }
}

/** Hasil membuka berkas: berkas kerja atau pesan kesalahan, serta isi teks atau ukurannya. */
private class Opened(
    val work: Workfile?,
    val text: String = "",
    val error: String? = null,
    val size: Long = 0L,
)

/** Layar editor: teks biasa atau hex, tergantung pilihan di menu. */
@Composable
fun EditorScreen(target: EditorTarget, onBack: () -> Unit) {
    if (target.hex) {
        HexEditor(target, onBack)
    } else {
        TextEditor(target, onBack)
    }
}

/** Menyiapkan berkas kerja. Null jika berkas tidak bisa dibaca dan diubah. */
private fun prepareWorkfile(context: Context, path: String, root: Boolean): Workfile? {
    val original = File(path)
    if (original.canRead() && original.canWrite()) return Workfile(original, isCopy = false)
    if (!root) return null

    val copy = File(context.cacheDir, "editor_${System.currentTimeMillis()}")
    val result = RootShell.run(
        "cp ${RootShell.quote(path)} ${RootShell.quote(copy.path)} && chmod 666 ${RootShell.quote(copy.path)}",
    )
    return if (result.exitCode == 0 && copy.canRead() && copy.canWrite()) {
        Workfile(copy, isCopy = true)
    } else {
        copy.delete()
        null
    }
}

/** Mengembalikan salinan kerja ke berkas asli lewat root. Berkas yang langsung diubah tidak perlu ini. */
private fun commitWorkfile(work: Workfile, path: String): Boolean =
    !work.isCopy || RootShell.run("cp ${RootShell.quote(work.file.path)} ${RootShell.quote(path)}").exitCode == 0

private fun openForText(context: Context, target: EditorTarget): Opened {
    val work = prepareWorkfile(context, target.path, target.root)
        ?: return Opened(null, error = context.getString(R.string.editor_cannot_open))
    if (work.file.length() > MAX_TEXT_BYTES) {
        work.discard()
        return Opened(null, error = context.getString(R.string.editor_too_big))
    }
    return try {
        // Dekode dengan ketat: jika bukan UTF-8, teks akan rusak saat disimpan, jadi ditolak saja
        val text = Charsets.UTF_8.newDecoder()
            .decode(ByteBuffer.wrap(work.file.readBytes()))
            .toString()
        if (text.contains('\u0000')) throw CharacterCodingException()
        Opened(work, text = text)
    } catch (e: CharacterCodingException) {
        work.discard()
        Opened(null, error = context.getString(R.string.editor_binary))
    } catch (e: Exception) {
        work.discard()
        Opened(null, error = context.getString(R.string.editor_cannot_open))
    }
}

private fun openForHex(context: Context, target: EditorTarget): Opened {
    val work = prepareWorkfile(context, target.path, target.root)
        ?: return Opened(null, error = context.getString(R.string.editor_cannot_open))
    return Opened(work, size = work.file.length())
}

/** Membaca satu halaman (256 byte) mulai dari [page]. */
private fun readPage(file: File, page: Int): ByteArray {
    val start = page.toLong() * PAGE_SIZE
    return RandomAccessFile(file, "r").use { raf ->
        val length = minOf(PAGE_SIZE.toLong(), (raf.length() - start).coerceAtLeast(0L)).toInt()
        val buffer = ByteArray(length)
        if (length > 0) {
            raf.seek(start)
            raf.readFully(buffer)
        }
        buffer
    }
}

// ---------- Editor teks ----------

@Composable
private fun TextEditor(target: EditorTarget, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var work by remember { mutableStateOf<Workfile?>(null) }
    var text by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var confirmExit by remember { mutableStateOf(false) }

    val dirty = work != null && text != saved

    fun close() {
        work?.discard()
        onBack()
    }

    fun requestClose() {
        if (dirty) confirmExit = true else close()
    }

    BackHandler { requestClose() }

    LaunchedEffect(target) {
        val opened = withContext(Dispatchers.IO) { openForText(context, target) }
        work = opened.work
        text = opened.text
        saved = opened.text
        error = opened.error
        loading = false
    }

    fun save() {
        val current = work ?: return
        val content = text
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    current.file.writeBytes(content.toByteArray(Charsets.UTF_8))
                    commitWorkfile(current, target.path)
                } catch (e: Exception) {
                    false
                }
            }
            if (ok) {
                saved = content
                Toast.makeText(context, R.string.editor_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.editor_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val err = error
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EditorTopBar(
            title = FileRepository.nameOf(target.path),
            info = stringResource(R.string.editor_encoding),
            onBack = { requestClose() },
            canSave = work != null && !loading,
            onSave = { save() },
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CenterMessage(stringResource(R.string.editor_loading))
                err != null -> CenterMessage(err)
                // Kolom teks polos tanpa bingkai, memenuhi layar seperti editor NP Manager
                else -> BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Ink),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }

    if (confirmExit) {
        DiscardDialog(
            onKeep = { confirmExit = false },
            onDiscard = {
                confirmExit = false
                close()
            },
        )
    }
}

// ---------- Editor hex ----------

@Composable
private fun HexEditor(target: EditorTarget, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var work by remember { mutableStateOf<Workfile?>(null) }
    var size by remember { mutableStateOf(0L) }
    var page by remember { mutableStateOf(0) }
    var bytes by remember { mutableStateOf(ByteArray(0)) }
    // Perubahan belum disimpan, dengan kunci posisi byte di berkas
    val pending = remember { mutableStateMapOf<Long, Int>() }
    var editing by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var confirmExit by remember { mutableStateOf(false) }

    fun close() {
        work?.discard()
        onBack()
    }

    fun requestClose() {
        if (pending.isNotEmpty()) confirmExit = true else close()
    }

    BackHandler { requestClose() }

    LaunchedEffect(target) {
        val opened = withContext(Dispatchers.IO) { openForHex(context, target) }
        work = opened.work
        size = opened.size
        error = opened.error
        loading = false
    }

    LaunchedEffect(work, page) {
        val current = work ?: return@LaunchedEffect
        bytes = withContext(Dispatchers.IO) { readPage(current.file, page) }
    }

    fun save() {
        val current = work ?: return
        val changes = pending.toMap()
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    RandomAccessFile(current.file, "rw").use { raf ->
                        for ((offset, value) in changes) {
                            raf.seek(offset)
                            raf.write(value)
                        }
                    }
                    commitWorkfile(current, target.path)
                } catch (e: Exception) {
                    false
                }
            }
            if (ok) {
                pending.clear()
                bytes = withContext(Dispatchers.IO) { readPage(current.file, page) }
                Toast.makeText(context, R.string.editor_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.editor_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val start = page.toLong() * PAGE_SIZE
    // Isi halaman yang tampil, dengan perubahan yang belum disimpan ditimpa di atasnya
    val shown = List(bytes.size) { i -> pending[start + i] ?: (bytes[i].toInt() and 0xFF) }
    val rows = (shown.size + ROW_SIZE - 1) / ROW_SIZE
    val err = error

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        EditorTopBar(
            title = FileRepository.nameOf(target.path),
            info = stringResource(R.string.editor_hex_label),
            onBack = { requestClose() },
            canSave = work != null && !loading,
            onSave = { save() },
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CenterMessage(stringResource(R.string.editor_loading))
                err != null -> CenterMessage(err)
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    items(count = rows) { row ->
                        val from = row * ROW_SIZE
                        val to = minOf(from + ROW_SIZE, shown.size)
                        HexRow(
                            rowOffset = start + from,
                            values = shown.subList(from, to),
                            changed = { i -> pending.containsKey(start + from + i) },
                            onTap = { i -> editing = start + from + i },
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = LineColor)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = { page = page - 1 },
                enabled = page > 0 && work != null,
            ) {
                Text(stringResource(R.string.editor_prev))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val info = if (shown.isEmpty()) {
                    "0 byte"
                } else {
                    stringResource(
                        R.string.editor_page_info,
                        "%08X".format(start),
                        "%08X".format(start + shown.size - 1),
                        size.toString(),
                    )
                }
                Text(text = info, fontSize = 12.sp, color = Muted)
                if (pending.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_pending, pending.size),
                        fontSize = 12.sp,
                        color = Accent,
                    )
                }
            }
            TextButton(
                onClick = { page = page + 1 },
                enabled = work != null && (page + 1).toLong() * PAGE_SIZE < size,
            ) {
                Text(stringResource(R.string.editor_next))
            }
        }
    }

    // Dialog ubah satu byte
    val editOffset = editing
    if (editOffset != null) {
        val index = (editOffset - start).toInt()
        val current = pending[editOffset] ?: ((bytes.getOrNull(index)?.toInt() ?: 0) and 0xFF)
        HexEditDialog(
            offset = editOffset,
            current = current,
            onApply = { value ->
                pending[editOffset] = value
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (confirmExit) {
        DiscardDialog(
            onKeep = { confirmExit = false },
            onDiscard = {
                confirmExit = false
                close()
            },
        )
    }
}

@Composable
private fun HexRow(
    rowOffset: Long,
    values: List<Int>,
    changed: (Int) -> Boolean,
    onTap: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%08X".format(rowOffset),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Muted,
            modifier = Modifier.width(68.dp),
        )
        values.forEachIndexed { i, value ->
            val isChanged = changed(i)
            Text(
                text = "%02X".format(value),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = if (isChanged) FontWeight.Bold else FontWeight.Normal,
                color = if (isChanged) Accent else Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(24.dp)
                    .clickable { onTap(i) }
                    .padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = values.map { if (it in 32..126) it.toChar() else '.' }.joinToString(""),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Muted,
        )
    }
}

@Composable
private fun HexEditDialog(
    offset: Long,
    current: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("%02X".format(current)) }
    val parsed = if (input.isNotEmpty() && input.length <= 2 && input.all { it in HEX_CHARS }) {
        input.toInt(16)
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_hex_edit_title, "%08X".format(offset))) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.uppercase().take(2) },
                    singleLine = true,
                    isError = input.isNotEmpty() && parsed == null,
                    placeholder = { Text(stringResource(R.string.editor_hex_hint)) },
                )
                if (input.isNotEmpty() && parsed == null) {
                    Text(
                        text = stringResource(R.string.editor_hex_invalid),
                        color = Danger,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onApply) },
                enabled = parsed != null,
            ) {
                Text(stringResource(R.string.editor_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}

// ---------- Bagian bersama ----------

/**
 * Bilah atas editor seperti NP Manager: tombol kembali di kiri, ikon simpan di kanan,
 * lalu satu baris tipis berisi nama berkas dan keterangan (misalnya UTF-8).
 */
@Composable
private fun EditorTopBar(
    title: String,
    info: String,
    onBack: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBrush)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSave, enabled = canSave) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(R.string.btn_save),
                    tint = if (canSave) Color.White else Color.White.copy(alpha = 0.45f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = info, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Text(
        text = text,
        color = Muted,
        fontSize = 14.sp,
        modifier = Modifier.padding(24.dp),
    )
}

@Composable
private fun DiscardDialog(onKeep: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.editor_discard_title)) },
        text = { Text(stringResource(R.string.editor_discard_body)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.btn_discard), color = Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep) {
                Text(stringResource(R.string.editor_keep_editing))
            }
        },
    )
}
