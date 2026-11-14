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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
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
internal class Workfile(val file: File, val isCopy: Boolean) {
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

/**
 * Layar editor: teks biasa atau hex, tergantung pilihan di menu. Editor teks menampilkan berkas
 * yang sedang aktif di [EditorSession], jadi bisa berpindah di antara beberapa berkas yang terbuka.
 */
@Composable
fun EditorScreen(target: EditorTarget, onBack: () -> Unit) {
    if (target.hex) {
        HexEditor(target, onBack)
        return
    }
    val doc = EditorSession.current
    if (doc == null) {
        // Semua berkas sudah ditutup
        LaunchedEffect(Unit) { onBack() }
    } else {
        key(doc) { TextEditor(doc, onExit = onBack) }
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

// Simbol di baris bawah editor, seperti NP Manager. "Tab" menyisipkan empat spasi.
private val SYMBOLS = listOf("Tab", "<", ">", "=", "\"", ":", "@", "+", "-", "/", "(", ")", "{", "}", "[", "]", ";", ".", ",", "_", "#", "$", "&", "|", "!", "?", "*", "'")
private const val MAX_UNDO = 100

/** Paling banyak hasil pencarian yang dihitung, supaya berkas besar tidak membuat editor tersendat. */
private const val MAX_MATCHES = 5000

/** Di atas jumlah baris ini, nomor baris tidak lagi disejajarkan dengan baris yang terlipat. */
private const val MAX_GUTTER_LINES = 20_000

private val WORD = Regex("[A-Za-z_][A-Za-z0-9_]{2,}")

/** Pilihan bahasa di dialog "Sintaks": nama yang tampil dan akhiran berkas yang mewakilinya. */
private val SYNTAX_CHOICES = listOf(
    "XML / HTML" to "xml",
    "JSON" to "json",
    "Smali" to "smali",
    "Java / Kotlin / JavaScript / CSS / C" to "java",
    "Shell / Properties / YAML / Python" to "sh",
)

/**
 * Satu berkas yang sedang terbuka di editor teks. Isinya tetap tersimpan di sini selama editor
 * diperkecil atau berpindah ke berkas lain, jadi perubahan yang belum disimpan tidak hilang.
 */
class EditorDoc(val target: EditorTarget) {
    internal var work by mutableStateOf<Workfile?>(null)
    var value by mutableStateOf(TextFieldValue(""))
    var saved by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(true)
    internal var loaded = false
    val undoStack = mutableStateListOf<String>()
    val redoStack = mutableStateListOf<String>()

    /** True jika ada perubahan yang belum disimpan. */
    val dirty: Boolean get() = work != null && value.text != saved
}

/** Daftar berkas yang terbuka di editor teks, seperti daftar di sisi kiri editor MT Manager. */
object EditorSession {
    val docs = mutableStateListOf<EditorDoc>()
    var current by mutableStateOf<EditorDoc?>(null)

    /** Membuka [target], atau berpindah ke sana jika sudah terbuka. */
    fun open(target: EditorTarget): EditorDoc {
        val doc = docs.firstOrNull { it.target.path == target.path } ?: EditorDoc(target).also { docs.add(it) }
        current = doc
        return doc
    }

    fun close(doc: EditorDoc) {
        doc.work?.discard()
        docs.remove(doc)
        if (current === doc) current = docs.lastOrNull()
    }
}

private suspend fun loadDoc(context: Context, doc: EditorDoc) {
    doc.loading = true
    val opened = withContext(Dispatchers.IO) { openForText(context, doc.target) }
    doc.work = opened.work
    doc.value = TextFieldValue(opened.text)
    doc.saved = opened.text
    doc.error = opened.error
    doc.undoStack.clear()
    doc.redoStack.clear()
    doc.loading = false
}

/** Mencari semua kemunculan [query] di [text]. Pola regex yang salah tulis menghasilkan daftar kosong. */
private fun findMatches(text: String, query: String, matchCase: Boolean, wholeWord: Boolean, useRegex: Boolean): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    return try {
        val body = if (useRegex) query else Regex.escape(query)
        val pattern = if (wholeWord) "\\b(?:$body)\\b" else body
        val options = if (matchCase) setOf(RegexOption.MULTILINE) else setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
        Regex(pattern, options).findAll(text)
            .filter { !it.range.isEmpty() }
            .take(MAX_MATCHES)
            .map { it.range }
            .toList()
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Teks nomor baris. Jika [layout] diberikan (mode bungkus kata), baris yang terlipat diberi baris
 * kosong di bawah nomornya supaya nomor tetap sejajar dengan awal barisnya.
 */
private fun gutterText(text: String, lineCount: Int, layout: TextLayoutResult?): String {
    if (layout == null || lineCount > MAX_GUTTER_LINES || layout.layoutInput.text.length != text.length) {
        return (1..lineCount).joinToString("\n")
    }
    val out = StringBuilder()
    var number = 1
    var start = 0
    while (true) {
        val newline = text.indexOf('\n', start)
        val end = if (newline < 0) text.length else newline
        val folds = layout.getLineForOffset(end) - layout.getLineForOffset(start)
        out.append(number)
        repeat(folds) { out.append('\n') }
        if (newline < 0) break
        out.append('\n')
        start = newline + 1
        number++
    }
    return out.toString()
}

@Composable
private fun TextEditor(doc: EditorDoc, onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs.of(context) }
    val target = doc.target
    val vScroll = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var confirmClose by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var fileMenuOpen by remember { mutableStateOf(false) }
    var listOpen by remember { mutableStateOf(false) }
    var gotoOpen by remember { mutableStateOf(false) }
    var syntaxOpen by remember { mutableStateOf(false) }
    var prefsOpen by remember { mutableStateOf(false) }
    var saveAsOpen by remember { mutableStateOf(false) }
    // Pilihan dari menu titik tiga. Sebagian diingat untuk berkas berikutnya.
    var wrap by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_EDITOR_WRAP, true)) }
    var readOnly by remember { mutableStateOf(false) }
    var fluid by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_FLUID, false)) }
    var complete by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_EDITOR_COMPLETE, false)) }
    var fontSize by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_EDITOR_FONT, 13).coerceIn(9, 28)) }
    var showNumbers by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_EDITOR_NUMBERS, true)) }
    // Bahasa pewarnaan pilihan user. Null berarti mengikuti akhiran nama berkas.
    var syntaxExt by remember { mutableStateOf<String?>(null) }
    // Cari dan ganti
    var searchOpen by remember { mutableStateOf(false) }
    var replaceOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    // Riwayat posisi kursor untuk "Posisi sebelumnya" dan "Posisi berikutnya"
    val backJumps = remember { mutableStateListOf<Int>() }
    val forwardJumps = remember { mutableStateListOf<Int>() }

    val value = doc.value
    val text = value.text
    val ready = doc.work != null && !doc.loading
    val err = doc.error

    fun close() {
        EditorSession.close(doc)
        if (EditorSession.docs.isEmpty()) onExit()
    }

    fun requestClose() {
        if (doc.dirty) confirmClose = true else close()
    }

    BackHandler {
        if (searchOpen) searchOpen = false else requestClose()
    }

    LaunchedEffect(doc) {
        if (!doc.loaded) {
            doc.loaded = true
            loadDoc(context, doc)
        }
    }

    /** Mengganti isi editor dan mencatat isi lama supaya bisa diurungkan. */
    fun change(next: TextFieldValue) {
        if (next.text != doc.value.text) {
            doc.undoStack.add(doc.value.text)
            if (doc.undoStack.size > MAX_UNDO) doc.undoStack.removeAt(0)
            doc.redoStack.clear()
        }
        doc.value = next
    }

    fun undo() {
        val previous = doc.undoStack.removeLastOrNull() ?: return
        doc.redoStack.add(doc.value.text)
        doc.value = TextFieldValue(previous, TextRange(minOf(doc.value.selection.start, previous.length)))
    }

    fun redo() {
        val next = doc.redoStack.removeLastOrNull() ?: return
        doc.undoStack.add(doc.value.text)
        doc.value = TextFieldValue(next, TextRange(minOf(doc.value.selection.start, next.length)))
    }

    /** Menyisipkan simbol di posisi kursor (atau mengganti teks yang diblok). */
    fun insert(symbol: String) {
        val piece = if (symbol == "Tab") "    " else symbol
        val from = doc.value.selection.min
        val to = doc.value.selection.max
        change(TextFieldValue(doc.value.text.replaceRange(from, to, piece), TextRange(from + piece.length)))
    }

    /** Menggulir layar sampai posisi [offset] terlihat. */
    fun scrollTo(offset: Int) {
        val result = layout ?: return
        val safe = offset.coerceIn(0, result.layoutInput.text.length)
        val top = result.getLineTop(result.getLineForOffset(safe))
        scope.launch { vScroll.animateScrollTo((top - 160f).toInt().coerceAtLeast(0)) }
    }

    /** Memindahkan kursor ke [start] (dan memblok sampai [end]), sambil mencatat posisi lama. */
    fun jumpTo(start: Int, end: Int = start) {
        backJumps.add(doc.value.selection.start)
        if (backJumps.size > 50) backJumps.removeAt(0)
        forwardJumps.clear()
        val length = doc.value.text.length
        doc.value = doc.value.copy(selection = TextRange(start.coerceIn(0, length), end.coerceIn(0, length)))
        scrollTo(start)
    }

    fun jumpBack() {
        val target = backJumps.removeLastOrNull() ?: return
        forwardJumps.add(doc.value.selection.start)
        val safe = target.coerceIn(0, doc.value.text.length)
        doc.value = doc.value.copy(selection = TextRange(safe))
        scrollTo(safe)
    }

    fun jumpForward() {
        val target = forwardJumps.removeLastOrNull() ?: return
        backJumps.add(doc.value.selection.start)
        val safe = target.coerceIn(0, doc.value.text.length)
        doc.value = doc.value.copy(selection = TextRange(safe))
        scrollTo(safe)
    }

    fun gotoLine(number: Int) {
        var offset = 0
        var line = 1
        val content = doc.value.text
        while (line < number) {
            val next = content.indexOf('\n', offset)
            if (next < 0) break
            offset = next + 1
            line++
        }
        jumpTo(offset)
    }

    val matches = remember(text, query, matchCase, wholeWord, useRegex, searchOpen) {
        if (searchOpen) findMatches(text, query, matchCase, wholeWord, useRegex) else emptyList()
    }

    /** Pindah ke hasil pencarian berikutnya atau sebelumnya. Di ujung, pencarian berputar ke ujung lain. */
    fun findStep(forward: Boolean) {
        if (matches.isEmpty()) return
        val selection = doc.value.selection
        val found = if (forward) {
            matches.firstOrNull { it.first >= selection.max && !(it.first == selection.min && it.last + 1 == selection.max) }
                ?: matches.first()
        } else {
            matches.lastOrNull { it.last + 1 <= selection.min } ?: matches.last()
        }
        jumpTo(found.first, found.last + 1)
    }

    /** Mengganti hasil yang sedang diblok, lalu pindah ke hasil berikutnya. */
    fun replaceOne() {
        val selection = doc.value.selection
        val hit = matches.firstOrNull { it.first == selection.min && it.last + 1 == selection.max }
        if (hit == null) {
            findStep(forward = true)
            return
        }
        val updated = doc.value.text.replaceRange(hit.first, hit.last + 1, replacement)
        change(TextFieldValue(updated, TextRange(hit.first + replacement.length)))
    }

    fun replaceAll() {
        if (matches.isEmpty()) return
        val builder = StringBuilder(doc.value.text)
        for (range in matches.asReversed()) builder.replace(range.first, range.last + 1, replacement)
        val count = matches.size
        change(TextFieldValue(builder.toString(), TextRange(0)))
        Toast.makeText(context, context.getString(R.string.editor_replaced, count), Toast.LENGTH_SHORT).show()
    }

    fun save() {
        val current = doc.work ?: return
        val content = doc.value.text
        val backup = prefs.getBoolean(AppPrefs.KEY_BACKUP, false)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    // Pengaturan "Buat file cadangan": isi lama disimpan sebagai nama.bak di folder yang sama
                    if (backup && !current.isCopy) {
                        current.file.copyTo(File(target.path + ".bak"), overwrite = true)
                    }
                    current.file.writeBytes(content.toByteArray(Charsets.UTF_8))
                    commitWorkfile(current, target.path)
                } catch (e: Exception) {
                    false
                }
            }
            if (ok) {
                doc.saved = content
                Toast.makeText(context, R.string.editor_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.editor_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Menulis isi editor ke berkas lain. Berkas yang sedang dibuka tidak berubah. */
    fun saveAs(path: String) {
        val content = doc.value.text
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    File(path).writeBytes(content.toByteArray(Charsets.UTF_8))
                    true
                } catch (e: Exception) {
                    false
                }
            }
            Toast.makeText(context, if (ok) R.string.editor_saved else R.string.editor_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // Posisi kursor "baris:kolom" untuk baris keterangan di bawah bilah atas
    val cursor = value.selection.start.coerceIn(0, text.length)
    val line = text.count(0, cursor, '\n') + 1
    val column = cursor - (text.lastIndexOf('\n', cursor - 1) + 1) + 1
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    val numbers = remember(text, lineCount, layout, wrap) { gutterText(text, lineCount, if (wrap) layout else null) }
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.4f).sp,
        color = Ink,
    )
    // Pewarnaan kode menurut jenis berkas. Dimatikan saat "Mode halus" dinyalakan.
    val coloring = remember(target.path, fluid, syntaxExt) {
        if (fluid) VisualTransformation.None else SyntaxTransformation(syntaxExt ?: target.path.substringAfterLast('.', ""))
    }

    // Pelengkap kode: kata lain di berkas ini yang diawali huruf-huruf yang sedang diketik
    val wordStart = remember(text, cursor) {
        var index = cursor
        while (index > 0 && (text[index - 1].isLetterOrDigit() || text[index - 1] == '_')) index--
        index
    }
    val suggestions = remember(text, cursor, complete, readOnly) {
        val prefix = text.substring(wordStart, cursor)
        if (!complete || readOnly || prefix.length < 2 || text.length > HIGHLIGHT_LIMIT) {
            emptyList()
        } else {
            WORD.findAll(text).map { it.value }
                .filter { it.length > prefix.length && it.startsWith(prefix) }
                .distinct()
                .take(8)
                .toList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Body).imePadding()) {
        // Bilah atas seperti MT Manager: daftar berkas, urung, ulang, simpan, pensil, dan menu titik tiga
        Column(modifier = Modifier.fillMaxWidth().background(HeaderBrush).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { requestClose() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = Color.White)
                }
                IconButton(onClick = { listOpen = true }) {
                    Icon(Icons.Filled.Menu, stringResource(R.string.editor_open_files), tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                val canUndo = ready && !readOnly && doc.undoStack.isNotEmpty()
                val canRedo = ready && !readOnly && doc.redoStack.isNotEmpty()
                IconButton(onClick = { undo() }, enabled = canUndo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        stringResource(R.string.editor_undo),
                        tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.45f),
                    )
                }
                IconButton(onClick = { redo() }, enabled = canRedo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        stringResource(R.string.editor_redo),
                        tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.45f),
                    )
                }
                IconButton(onClick = { save() }, enabled = ready) {
                    Icon(
                        Icons.Filled.Save,
                        stringResource(R.string.btn_save),
                        tint = if (ready) Color.White else Color.White.copy(alpha = 0.45f),
                    )
                }
                // Pensil: berpindah antara mode ubah dan mode hanya baca
                IconButton(onClick = { readOnly = !readOnly }, enabled = ready) {
                    Icon(
                        if (readOnly) Icons.Filled.EditOff else Icons.Filled.Edit,
                        stringResource(R.string.editor_read_only),
                        tint = Color.White,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.cd_more), tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = DialogBg,
                    ) {
                        EditorMenuItem(stringResource(R.string.editor_menu_file), Icons.Filled.InsertDriveFile, arrow = true) {
                            menuOpen = false
                            fileMenuOpen = true
                        }
                        EditorMenuItem(stringResource(R.string.menu_search), Icons.Filled.Search) {
                            menuOpen = false
                            searchOpen = true
                        }
                        EditorMenuItem(stringResource(R.string.editor_syntax), Icons.Filled.Code) {
                            menuOpen = false
                            syntaxOpen = true
                        }
                        EditorMenuItem(
                            stringResource(R.string.editor_pos_prev),
                            Icons.AutoMirrored.Filled.ArrowBack,
                            enabled = backJumps.isNotEmpty(),
                        ) {
                            menuOpen = false
                            jumpBack()
                        }
                        EditorMenuItem(
                            stringResource(R.string.editor_pos_next),
                            Icons.AutoMirrored.Filled.ArrowForward,
                            enabled = forwardJumps.isNotEmpty(),
                        ) {
                            menuOpen = false
                            jumpForward()
                        }
                        EditorMenuItem(stringResource(R.string.editor_goto_line), Icons.Filled.FormatListNumbered) {
                            menuOpen = false
                            gotoOpen = true
                        }
                        EditorMenuItem(stringResource(R.string.editor_wrap), Icons.Filled.WrapText, checked = wrap) {
                            wrap = !wrap
                            prefs.edit().putBoolean(AppPrefs.KEY_EDITOR_WRAP, wrap).apply()
                        }
                        EditorMenuItem(stringResource(R.string.editor_read_only), Icons.Filled.Visibility, checked = readOnly) {
                            readOnly = !readOnly
                        }
                        EditorMenuItem(stringResource(R.string.editor_smooth), Icons.Filled.Speed, checked = fluid) {
                            fluid = !fluid
                            prefs.edit().putBoolean(AppPrefs.KEY_FLUID, fluid).apply()
                        }
                        EditorMenuItem(stringResource(R.string.editor_complete), Icons.Filled.Spellcheck, checked = complete) {
                            complete = !complete
                            prefs.edit().putBoolean(AppPrefs.KEY_EDITOR_COMPLETE, complete).apply()
                        }
                        EditorMenuItem(stringResource(R.string.editor_prefs), Icons.Filled.Settings) {
                            menuOpen = false
                            prefsOpen = true
                        }
                        EditorMenuItem(stringResource(R.string.editor_close_file), Icons.Filled.Close) {
                            menuOpen = false
                            requestClose()
                        }
                    }
                    // Isi menu "File"
                    DropdownMenu(
                        expanded = fileMenuOpen,
                        onDismissRequest = { fileMenuOpen = false },
                        containerColor = DialogBg,
                    ) {
                        EditorMenuItem(stringResource(R.string.btn_save), Icons.Filled.Save, enabled = ready) {
                            fileMenuOpen = false
                            save()
                        }
                        EditorMenuItem(stringResource(R.string.editor_save_as), Icons.Filled.SaveAs, enabled = ready) {
                            fileMenuOpen = false
                            saveAsOpen = true
                        }
                        EditorMenuItem(stringResource(R.string.editor_reload), Icons.Filled.Refresh) {
                            fileMenuOpen = false
                            scope.launch {
                                doc.work?.discard()
                                loadDoc(context, doc)
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // Tanda bintang berarti ada perubahan yang belum disimpan, seperti MT Manager
                    text = (if (doc.dirty) "*" else "") + FileRepository.nameOf(target.path),
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$line:$column  " + stringResource(R.string.editor_encoding),
                    color = Color.White,
                    fontSize = 11.sp,
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopStart,
        ) {
            when {
                doc.loading -> CenterMessage(stringResource(R.string.editor_loading))
                err != null -> CenterMessage(err)
                // Nomor baris di kiri dan teks di kanan digulir bersama
                else -> Row(modifier = Modifier.fillMaxSize().verticalScroll(vScroll)) {
                    if (showNumbers) {
                        Text(
                            text = numbers,
                            style = style,
                            color = Disabled,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .background(SoftGray)
                                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        )
                    }
                    if (wrap) {
                        // Bungkus kata: baris panjang dilipat ke bawah
                        BasicTextField(
                            value = value,
                            onValueChange = { change(it) },
                            readOnly = readOnly,
                            textStyle = style,
                            visualTransformation = coloring,
                            onTextLayout = { layout = it },
                            cursorBrush = SolidColor(Accent),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 300.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    } else {
                        // Tanpa bungkus kata: baris panjang digeser ke samping
                        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            BasicTextField(
                                value = value,
                                onValueChange = { change(it) },
                                readOnly = readOnly,
                                textStyle = style,
                                visualTransformation = coloring,
                                onTextLayout = { layout = it },
                                cursorBrush = SolidColor(Accent),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 360.dp, minHeight = 300.dp)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        if (ready && err == null) {
            // Saran pelengkap kode
            if (suggestions.isNotEmpty()) {
                HorizontalDivider(color = LineColor)
                Row(modifier = Modifier.fillMaxWidth().background(SoftGray).horizontalScroll(rememberScrollState())) {
                    suggestions.forEach { word ->
                        Text(
                            text = word,
                            color = Ink,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable {
                                    val current = doc.value
                                    val end = current.selection.start.coerceIn(wordStart, current.text.length)
                                    change(
                                        TextFieldValue(
                                            current.text.replaceRange(wordStart, end, word),
                                            TextRange(wordStart + word.length),
                                        ),
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (!readOnly) {
                HorizontalDivider(color = LineColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    SYMBOLS.forEach { symbol ->
                        Text(
                            text = symbol,
                            color = Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { insert(symbol) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            // Panel cari dan ganti di bawah, seperti MT Manager
            if (searchOpen) {
                HorizontalDivider(color = LineColor)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.editor_results, matches.size),
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    SearchToggle("Cc", matchCase) { matchCase = !matchCase }
                    SearchToggle("W", wholeWord) { wholeWord = !wholeWord }
                    SearchToggle(".*", useRegex) { useRegex = !useRegex }
                }
                SearchInput(stringResource(R.string.editor_find), query) { query = it }
                if (replaceOpen) {
                    SearchInput(stringResource(R.string.editor_replace_with), replacement) { replacement = it }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchAction(stringResource(R.string.editor_prev), matches.isNotEmpty()) { findStep(forward = false) }
                    SearchAction(stringResource(R.string.editor_find_next), matches.isNotEmpty()) { findStep(forward = true) }
                    // Ketukan pertama membuka kolom pengganti, ketukan berikutnya mengganti hasil yang diblok
                    SearchAction(stringResource(R.string.editor_replace), !readOnly) {
                        if (replaceOpen) replaceOne() else replaceOpen = true
                    }
                    SearchAction(stringResource(R.string.editor_replace_all), replaceOpen && !readOnly && matches.isNotEmpty()) {
                        replaceAll()
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { searchOpen = false }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.cd_close), tint = Muted)
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }

    if (confirmClose) {
        DiscardDialog(
            onKeep = { confirmClose = false },
            onDiscard = {
                confirmClose = false
                close()
            },
        )
    }

    if (gotoOpen) {
        InputDialog(
            title = stringResource(R.string.editor_goto_line),
            label = "1 - $lineCount",
            initial = line.toString(),
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                gotoOpen = false
                typed.toIntOrNull()?.let { gotoLine(it.coerceIn(1, lineCount)) }
            },
            onDismiss = { gotoOpen = false },
        )
    }

    if (saveAsOpen) {
        InputDialog(
            title = stringResource(R.string.editor_save_as),
            label = stringResource(R.string.dialog_jump_hint),
            initial = target.path,
            confirmText = stringResource(R.string.btn_save),
            onConfirm = { typed ->
                saveAsOpen = false
                if (typed.startsWith("/")) saveAs(typed)
            },
            onDismiss = { saveAsOpen = false },
        )
    }

    if (syntaxOpen) {
        AlertDialog(
            onDismissRequest = { syntaxOpen = false },
            containerColor = DialogBg,
            title = { Text(stringResource(R.string.editor_syntax), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    // Null = mengikuti nama berkas, kosong = tanpa pewarnaan
                    val choices: List<Pair<String, String?>> = listOf<Pair<String, String?>>(
                        stringResource(R.string.editor_syntax_auto) to null,
                        stringResource(R.string.editor_syntax_none) to "",
                    ) + SYNTAX_CHOICES
                    choices.forEach { (label, ext) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    syntaxExt = ext
                                    syntaxOpen = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = syntaxExt == ext,
                                onClick = {
                                    syntaxExt = ext
                                    syntaxOpen = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Accent),
                            )
                            Text(label, fontSize = 14.sp, color = Ink)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { syntaxOpen = false }) {
                    Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    if (prefsOpen) {
        AlertDialog(
            onDismissRequest = { prefsOpen = false },
            containerColor = DialogBg,
            title = { Text(stringResource(R.string.editor_prefs), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.editor_font_size), fontSize = 14.sp, color = Ink, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            fontSize = (fontSize - 1).coerceAtLeast(9)
                            prefs.edit().putInt(AppPrefs.KEY_EDITOR_FONT, fontSize).apply()
                        }) { Text("-", color = Accent, fontSize = 18.sp) }
                        Text(fontSize.toString(), fontSize = 15.sp, color = Ink)
                        TextButton(onClick = {
                            fontSize = (fontSize + 1).coerceAtMost(28)
                            prefs.edit().putInt(AppPrefs.KEY_EDITOR_FONT, fontSize).apply()
                        }) { Text("+", color = Accent, fontSize = 18.sp) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            showNumbers = !showNumbers
                            prefs.edit().putBoolean(AppPrefs.KEY_EDITOR_NUMBERS, showNumbers).apply()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.editor_line_numbers), fontSize = 14.sp, color = Ink, modifier = Modifier.weight(1f))
                        Icon(
                            if (showNumbers) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (showNumbers) Ink else Muted,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { prefsOpen = false }) {
                    Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    // Daftar berkas yang sedang terbuka. "Perkecil" kembali ke pengelola berkas tanpa menutup apa pun.
    if (listOpen) {
        AlertDialog(
            onDismissRequest = { listOpen = false },
            containerColor = DialogBg,
            title = { Text(stringResource(R.string.tool_text_editor), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    EditorSession.docs.toList().forEach { other ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (other === doc) SelectTint else Color.Transparent)
                                .clickable {
                                    listOpen = false
                                    EditorSession.current = other
                                }
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                                Text(
                                    text = (if (other.dirty) "*" else "") + FileRepository.nameOf(other.target.path),
                                    color = Ink,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = FileRepository.parentOf(other.target.path),
                                    color = Muted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Berkas dengan perubahan yang belum disimpan tidak ditutup dari sini
                            IconButton(onClick = { EditorSession.close(other) }, enabled = !other.dirty && other !== doc) {
                                Icon(
                                    Icons.Filled.Close,
                                    stringResource(R.string.editor_close_file),
                                    tint = if (!other.dirty && other !== doc) Muted else Disabled,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    listOpen = false
                    onExit()
                }) {
                    Text(stringResource(R.string.editor_minimize), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { listOpen = false }) {
                    Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

/** Satu baris menu titik tiga editor. [checked] menampilkan kotak centang, [arrow] menampilkan panah submenu. */
@Composable
private fun EditorMenuItem(
    label: String,
    icon: ImageVector,
    checked: Boolean? = null,
    arrow: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val trailing: (@Composable () -> Unit)? = when {
        checked != null -> {
            {
                Icon(
                    imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (checked) Ink else Muted,
                )
            }
        }
        arrow -> {
            { Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = Muted) }
        }
        else -> null
    }
    DropdownMenuItem(
        text = { Text(text = label, color = if (enabled) Ink else Disabled, fontSize = 15.sp) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = if (enabled) MenuIcon else Disabled) },
        trailingIcon = trailing,
    )
}

/** Tombol kecil di panel cari: huruf besar-kecil, kata utuh, atau regex. Tebal saat aktif. */
@Composable
private fun SearchToggle(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (active) Ink else Disabled,
        fontSize = 13.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Kolom isian satu baris di panel cari, dengan garis bawah. */
@Composable
private fun SearchInput(label: String, value: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(92.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Ink),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
        }
        HorizontalDivider(color = Accent)
    }
}

@Composable
private fun SearchAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = label.uppercase(),
            color = if (enabled) Accent else Disabled,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Jumlah [ch] di antara posisi [from] (termasuk) dan [to] (tidak termasuk). */
private fun String.count(from: Int, to: Int, ch: Char): Int {
    var total = 0
    for (i in from until to) if (this[i] == ch) total++
    return total
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

    Column(modifier = Modifier.fillMaxSize().background(Body)) {
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
    canUndo: Boolean = false,
    onUndo: (() -> Unit)? = null,
    canRedo: Boolean = false,
    onRedo: (() -> Unit)? = null,
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
            // Urung dan ulang hanya ada di editor teks
            if (onUndo != null) {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.editor_undo),
                        tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.45f),
                    )
                }
            }
            if (onRedo != null) {
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.editor_redo),
                        tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.45f),
                    )
                }
            }
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
