package com.filemate.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Xml
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import com.filemate.app.data.RootShell
import com.filemate.app.net.KeepAliveService
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

// Ketiga alat di berkas ini memakai akses root, bukan izin aksesibilitas atau tampil di atas aplikasi lain.

// ---------- Perekaman aktivitas ----------

/**
 * Perekam aktivitas: setiap detik menanyakan ke sistem (lewat root) layar mana yang sedang tampil,
 * dan mencatatnya setiap kali berubah. Tetap berjalan saat Filemate ditinggal ke aplikasi lain.
 */
object ActivityRecorder {
    val running = MutableStateFlow(false)
    val log = MutableStateFlow<List<String>>(emptyList())

    /** True jika perekaman gagal dimulai karena akses root tidak ada. */
    val noRoot = MutableStateFlow(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start(context: Context) {
        if (running.value) return
        running.value = true
        noRoot.value = false
        KeepAliveService.acquire(context, "activity")
        job = scope.launch {
            if (!RootShell.isGranted()) {
                noRoot.value = true
                running.value = false
                KeepAliveService.release(context, "activity")
                return@launch
            }
            var last = ""
            while (isActive) {
                val output = RootShell.run("dumpsys activity activities | grep -E 'ResumedActivity' | head -n 3").output
                val match = Regex("([A-Za-z0-9_.]+)/([A-Za-z0-9_.\\$]+)").find(output)
                if (match != null) {
                    val pkg = match.groupValues[1]
                    val name = match.groupValues[2]
                    // ".Nama" adalah bentuk singkat dari "paket.Nama"
                    val full = pkg + "\n" + (if (name.startsWith(".")) pkg + name else name)
                    if (full != last) {
                        last = full
                        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                        log.update { (listOf("$time\n$full") + it).take(500) }
                    }
                }
                delay(1000)
            }
        }
    }

    fun stop(context: Context) {
        job?.cancel()
        job = null
        if (running.value) KeepAliveService.release(context, "activity")
        running.value = false
    }
}

@Composable
fun ActivityRecordScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val clipboard = LocalClipboardManager.current
    val running by ActivityRecorder.running.collectAsState()
    val noRoot by ActivityRecorder.noRoot.collectAsState()
    val log by ActivityRecorder.log.collectAsState()
    val copied = stringResource(R.string.tool_copied)

    ToolScaffold(stringResource(R.string.tool_activity_record), onBack) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(stringResource(R.string.act_desc), color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.padding(top = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(if (running) R.string.act_stop else R.string.act_start)) {
                    if (running) ActivityRecorder.stop(context) else ActivityRecorder.start(context)
                }
                PillButton(stringResource(R.string.act_clear), enabled = log.isNotEmpty()) {
                    ActivityRecorder.log.value = emptyList()
                }
            }
            if (noRoot) {
                Spacer(Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.tool_need_root), color = Danger, fontSize = 13.sp)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).navigationBarsPadding()) {
            items(log) { line ->
                Text(
                    text = line,
                    color = Ink,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Yang disalin adalah nama aktivitas lengkap (baris terakhir)
                            clipboard.setText(AnnotatedString(line.substringAfterLast('\n')))
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                )
            }
        }
    }
}

// ---------- Hitung mundur bersama ----------

/**
 * Tombol hitung mundur: memberi waktu untuk pindah ke aplikasi yang ingin diambil,
 * lalu menjalankan [capture] di latar belakang dan menyerahkan hasilnya ke [onResult].
 */
@Composable
private fun <T> CaptureButtons(capture: () -> T?, onResult: (T?) -> Unit) {
    val scope = rememberCoroutineScope()
    var countdown by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    fun go(seconds: Int) {
        busy = true
        scope.launch {
            for (left in seconds downTo 1) {
                countdown = left
                delay(1000)
            }
            countdown = 0
            val result = withContext(Dispatchers.IO) {
                try {
                    capture()
                } catch (e: Exception) {
                    null
                }
            }
            busy = false
            onResult(result)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PillButton(stringResource(R.string.cap_in, 3), enabled = !busy) { go(3) }
        PillButton(stringResource(R.string.cap_in, 5), enabled = !busy) { go(5) }
        if (busy) {
            Text(
                text = if (countdown > 0) countdown.toString() else stringResource(R.string.net_working),
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ---------- Penampil tata letak ----------

private class LayoutNode(val depth: Int, val type: String, val text: String, val id: String, val bounds: String)

/** Mengambil susunan tampilan layar yang sedang terbuka lewat perintah uiautomator (butuh root). */
private fun dumpLayout(context: Context): List<LayoutNode>? {
    val file = File(context.cacheDir, "layout.xml")
    file.delete()
    val path = RootShell.quote(file.path)
    RootShell.run("uiautomator dump $path && chmod 666 $path")
    if (!file.isFile) return null
    val nodes = ArrayList<LayoutNode>()
    val parser = Xml.newPullParser()
    parser.setInput(StringReader(file.readText()))
    var depth = 0
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name == "node") {
            nodes.add(
                LayoutNode(
                    depth = depth,
                    type = parser.getAttributeValue(null, "class").orEmpty().substringAfterLast('.'),
                    text = parser.getAttributeValue(null, "text").orEmpty(),
                    id = parser.getAttributeValue(null, "resource-id").orEmpty().substringAfter(":id/"),
                    bounds = parser.getAttributeValue(null, "bounds").orEmpty(),
                ),
            )
            depth++
        } else if (event == XmlPullParser.END_TAG && parser.name == "node") {
            depth--
        }
        event = parser.next()
    }
    return nodes
}

@Composable
fun LayoutViewerScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    var nodes by remember { mutableStateOf<List<LayoutNode>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }

    ToolScaffold(stringResource(R.string.tool_layout_viewer), onBack) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(stringResource(R.string.layout_desc), color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.padding(top = 10.dp))
            CaptureButtons(capture = { dumpLayout(context) }) { result ->
                failed = result == null
                nodes = result ?: emptyList()
            }
            if (failed) {
                Spacer(Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.tool_need_root), color = Danger, fontSize = 13.sp)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).navigationBarsPadding()) {
            items(nodes) { node ->
                // Semakin dalam letak sebuah elemen, semakin menjorok ke kanan
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (12 + node.depth.coerceAtMost(14) * 10).dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                ) {
                    Text(
                        text = node.type + if (node.id.isNotEmpty()) "  #${node.id}" else "",
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (node.text.isNotEmpty()) {
                        Text(text = "\"${node.text}\"", color = Ink, fontSize = 12.sp, maxLines = 2)
                    }
                    Text(text = node.bounds, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ---------- Penampil mata layar (pengambil warna) ----------

/** Mengambil tangkapan layar lewat perintah screencap (butuh root). */
private fun captureScreen(context: Context): Bitmap? {
    val file = File(context.cacheDir, "screen.png")
    file.delete()
    val path = RootShell.quote(file.path)
    RootShell.run("screencap -p $path && chmod 666 $path")
    if (!file.isFile) return null
    return BitmapFactory.decodeFile(file.path)
}

@Composable
fun ColorPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val clipboard = LocalClipboardManager.current
    var shot by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val copied = stringResource(R.string.tool_copied)

    /** Mengubah titik sentuh di layar menjadi piksel pada gambar, lalu membaca warnanya. */
    fun pick(point: Offset) {
        val image = shot ?: return
        if (box.width == 0 || box.height == 0) return
        val scale = minOf(box.width.toFloat() / image.width, box.height.toFloat() / image.height)
        val left = (box.width - image.width * scale) / 2f
        val top = (box.height - image.height * scale) / 2f
        val x = ((point.x - left) / scale).toInt()
        val y = ((point.y - top) / scale).toInt()
        if (x in 0 until image.width && y in 0 until image.height) picked = image.getPixel(x, y)
    }

    ToolScaffold(stringResource(R.string.tool_color_picker), onBack) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(stringResource(R.string.color_desc), color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.padding(top = 10.dp))
            CaptureButtons(capture = { captureScreen(context) }) { result ->
                failed = result == null
                shot = result
                picked = null
            }
            if (failed) {
                Spacer(Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.tool_need_root), color = Danger, fontSize = 13.sp)
            }
            val color = picked
            if (color != null) {
                val hex = "#%06X".format(color and 0xFFFFFF)
                val red = (color shr 16) and 0xFF
                val green = (color shr 8) and 0xFF
                val blue = color and 0xFF
                Spacer(Modifier.padding(top = 10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(hex))
                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(color))
                            .border(1.dp, LineColor),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "$hex   RGB($red, $green, $blue)",
                        color = Ink,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        val image = shot
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(SoftGray)
                    .onSizeChanged { box = it }
                    .pointerInput(image) { detectTapGestures { pick(it) } }
                    .pointerInput(image) { detectDragGestures { change, _ -> pick(change.position) } },
            )
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxSize())
        }
    }
}
