package com.filemate.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.filemate.app.R
import com.filemate.app.data.RootShell
import com.filemate.app.mcp.McpService
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Alat yang dibuka dari menu samping sebagai layar penuh. */
enum class ToolKind {
    TOOLBOX, TIMESTAMP, SMALI, RULER, NETWORK, DECIBEL, LAYOUT, COLOR, CLASSES, FTP, HTTP, LAN, FONT, KEYS, PASSWORDS,
}

/** Kerangka layar alat: bilah atas dengan tombol kembali dan judul, lalu isi di bawahnya. */
@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler { onBack() }
    Column(modifier = Modifier.fillMaxSize().background(Body).imePadding()) {
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
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        content()
    }
}

/** Tombol pil abu-abu yang dipakai di layar alat. */
@Composable
fun PillButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (enabled) Ink else Disabled,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(IconBox)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Kotak hasil berhuruf mesin tik yang bisa diblok dan disalin. */
@Composable
private fun ResultBox(text: String, modifier: Modifier = Modifier) {
    SelectionContainer(modifier = modifier) {
        Text(
            text = text,
            color = Ink,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftGray, RoundedCornerShape(10.dp))
                .padding(12.dp),
        )
    }
}

// ---------- Toolbox ----------

private fun hash(algorithm: String, text: String): String =
    MessageDigest.getInstance(algorithm).digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

/** Semua pengubah teks di Toolbox: nama tombol (dari strings) dan fungsinya. */
private fun op(label: Int, convert: (String) -> String): Pair<Int, (String) -> String> = Pair(label, convert)

private val TOOLBOX_OPS: List<Pair<Int, (String) -> String>> = listOf(
    op(R.string.tb_base64_enc) { s -> Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP) },
    op(R.string.tb_base64_dec) { s -> String(Base64.decode(s.trim(), Base64.DEFAULT), Charsets.UTF_8) },
    op(R.string.tb_url_enc) { s -> URLEncoder.encode(s, "UTF-8") },
    op(R.string.tb_url_dec) { s -> URLDecoder.decode(s, "UTF-8") },
    op(R.string.tb_hex_enc) { s -> s.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) } },
    op(R.string.tb_hex_dec) { s ->
        val clean = s.filter { it.isLetterOrDigit() }
        String(ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }, Charsets.UTF_8)
    },
    op(R.string.tb_unicode_enc) { s ->
        s.map { ch -> if (ch.code < 128) ch.toString() else "\\u%04x".format(ch.code) }.joinToString("")
    },
    op(R.string.tb_unicode_dec) { s ->
        Regex("\\\\u([0-9a-fA-F]{4})").replace(s) { m -> m.groupValues[1].toInt(16).toChar().toString() }
    },
    op(R.string.tb_md5) { s -> hash("MD5", s) },
    op(R.string.tb_sha1) { s -> hash("SHA-1", s) },
    op(R.string.tb_sha256) { s -> hash("SHA-256", s) },
    op(R.string.tb_upper) { s -> s.uppercase() },
    op(R.string.tb_lower) { s -> s.lowercase() },
    op(R.string.tb_strip) { s -> s.filterNot { it.isWhitespace() } },
    op(R.string.tb_reverse) { s -> s.reversed() },
)

/** Toolbox: mengubah teks ke Base64, URL, heksadesimal, Unicode, MD5, dan sejenisnya. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolboxScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val failed = stringResource(R.string.tb_failed)
    val copied = stringResource(R.string.tool_copied)

    ToolScaffold(stringResource(R.string.tool_toolbox), onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(14.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.tb_input)) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TOOLBOX_OPS.forEach { (label, op) ->
                    PillButton(stringResource(label)) {
                        output = try {
                            op(input)
                        } catch (e: Exception) {
                            failed
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.tb_output), color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            ResultBox(output)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(R.string.tool_copy), enabled = output.isNotEmpty()) {
                    clipboard.setText(AnnotatedString(output))
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                }
                // Hasil dijadikan masukan lagi, untuk mengubah berantai
                PillButton(stringResource(R.string.tb_use_as_input), enabled = output.isNotEmpty()) {
                    input = output
                }
            }
        }
    }
}

// ---------- Konversi stempel waktu ----------

private const val DATE_PATTERN = "yyyy-MM-dd HH:mm:ss"

/** Konversi stempel waktu: angka detik atau milidetik ke tanggal, dan sebaliknya. */
@Composable
fun TimestampScreen(onBack: () -> Unit) {
    var stamp by remember { mutableStateOf((System.currentTimeMillis() / 1000).toString()) }
    var date by remember { mutableStateOf(SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date())) }
    var stampResult by remember { mutableStateOf("") }
    var dateResult by remember { mutableStateOf("") }
    val invalid = stringResource(R.string.ts_invalid)

    ToolScaffold(stringResource(R.string.tool_timestamp), onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(14.dp),
        ) {
            OutlinedTextField(
                value = stamp,
                onValueChange = { stamp = it.filter { ch -> ch.isDigit() }.take(13) },
                label = { Text(stringResource(R.string.ts_stamp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(R.string.ts_to_date)) {
                    val number = stamp.toLongOrNull()
                    stampResult = if (number == null) {
                        invalid
                    } else {
                        // Lebih dari 10 angka berarti milidetik
                        val millis = if (stamp.length > 10) number else number * 1000
                        SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date(millis))
                    }
                }
                PillButton(stringResource(R.string.ts_now)) {
                    stamp = (System.currentTimeMillis() / 1000).toString()
                    date = SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date())
                }
            }
            Spacer(Modifier.height(8.dp))
            ResultBox(stampResult)

            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text(stringResource(R.string.ts_date, DATE_PATTERN)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            PillButton(stringResource(R.string.ts_to_stamp)) {
                dateResult = try {
                    val format = SimpleDateFormat(DATE_PATTERN, Locale.US)
                    format.isLenient = false
                    val millis = format.parse(date.trim())?.time ?: throw IllegalArgumentException()
                    "${millis / 1000}\n$millis"
                } catch (e: Exception) {
                    invalid
                }
            }
            Spacer(Modifier.height(8.dp))
            ResultBox(dateResult)
        }
    }
}

// ---------- Kueri sintaks Smali ----------

/** Kueri sintaks Smali: daftar perintah Smali dengan penjelasannya, bisa dicari. */
@Composable
fun SmaliQueryScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = SMALI_SYNTAX.filter {
        query.isBlank() || it.first.contains(query, ignoreCase = true) || it.second.contains(query, ignoreCase = true)
    }

    ToolScaffold(stringResource(R.string.tool_smali_query), onBack) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f).navigationBarsPadding()) {
            items(shown) { (syntax, meaning) ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = syntax,
                        color = Ink,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(text = meaning, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

// ---------- Penggaris layar ----------

/**
 * Penggaris layar: garis ukur sentimeter dan milimeter di tepi kiri layar.
 * Ketuk atau geser di layar untuk menaruh garis penanda dan melihat jaraknya dari atas.
 */
@Composable
fun RulerScreen(onBack: () -> Unit) {
    val metrics = LocalContext.current.resources.displayMetrics
    // Jumlah piksel per milimeter menurut data layar HP
    val pxPerMm = metrics.ydpi / 25.4f
    var marker by remember { mutableStateOf(0f) }
    val paint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 30f
            isAntiAlias = true
        }
    }

    ToolScaffold(stringResource(R.string.tool_ruler), onBack) {
        Text(
            text = stringResource(R.string.ruler_value, marker / pxPerMm, marker.toInt()),
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { marker = it.y.coerceAtLeast(0f) } }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> marker = change.position.y.coerceAtLeast(0f) }
                },
        ) {
            val totalMm = (size.height / pxPerMm).toInt()
            for (mm in 0..totalMm) {
                val y = mm * pxPerMm
                val length = when {
                    mm % 10 == 0 -> 70f
                    mm % 5 == 0 -> 46f
                    else -> 28f
                }
                drawLine(Ink, Offset(0f, y), Offset(length, y), strokeWidth = 2f)
                if (mm % 10 == 0 && mm > 0) {
                    drawContext.canvas.nativeCanvas.drawText((mm / 10).toString(), 80f, y + 10f, paint)
                }
            }
            drawLine(Danger, Offset(0f, marker), Offset(size.width, marker), strokeWidth = 3f)
        }
        Text(
            text = stringResource(R.string.ruler_note),
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// ---------- Alat jaringan ----------

/** Alat jaringan: alamat IP HP, ping, pencarian DNS, dan uji port. */
@Composable
fun NetworkToolsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("80") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val ip = remember { McpService.lanAddress() }
    val working = stringResource(R.string.net_working)
    val needHost = stringResource(R.string.net_need_host)
    val portOpen = stringResource(R.string.net_port_open)
    val portClosed = stringResource(R.string.net_port_closed)

    fun launchTask(block: () -> String) {
        if (host.isBlank()) {
            output = needHost
            return
        }
        busy = true
        output = working
        scope.launch {
            output = withContext(Dispatchers.IO) {
                try {
                    block()
                } catch (e: Exception) {
                    e.javaClass.simpleName + ": " + (e.message ?: "")
                }
            }
            busy = false
        }
    }

    ToolScaffold(stringResource(R.string.tool_network), onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(14.dp),
        ) {
            Text(
                text = stringResource(R.string.net_my_ip, ip ?: "-"),
                color = Ink,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text(stringResource(R.string.net_host)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text(stringResource(R.string.mcp_port)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(140.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Ping", enabled = !busy) {
                    val target = host
                    // Nama host hanya boleh huruf, angka, titik, dan tanda hubung, supaya aman dijalankan di shell
                    launchTask {
                        if (!target.matches(Regex("[A-Za-z0-9.:-]+"))) {
                            throw IllegalArgumentException(needHost)
                        }
                        RootShell.sh("ping -c 4 -W 3 $target").output.trim()
                    }
                }
                PillButton("DNS", enabled = !busy) {
                    val target = host
                    launchTask { InetAddress.getAllByName(target).joinToString("\n") { it.hostAddress ?: "" } }
                }
                PillButton(stringResource(R.string.net_check_port), enabled = !busy) {
                    val target = host
                    val number = port.toIntOrNull() ?: 80
                    launchTask {
                        try {
                            Socket().use { it.connect(InetSocketAddress(target, number), 3000) }
                            "$target:$number  $portOpen"
                        } catch (e: Exception) {
                            "$target:$number  $portClosed"
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            ResultBox(output)
        }
    }
}

// ---------- Pengukur desibel ----------

/**
 * Pengukur desibel: membaca kerasnya suara dari mikrofon. Angkanya perkiraan,
 * karena mikrofon tiap HP berbeda dan tidak dikalibrasi.
 */
@Composable
fun DecibelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var current by remember { mutableStateOf(0f) }
    var lowest by remember { mutableStateOf(0f) }
    var highest by remember { mutableStateOf(0f) }
    var average by remember { mutableStateOf(0f) }

    LaunchedEffect(granted) {
        if (!granted) {
            ask.launch(Manifest.permission.RECORD_AUDIO)
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val rate = 44100
            val size = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                .coerceAtLeast(4096)
            var recorder: AudioRecord? = null
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    size,
                )
                recorder.startRecording()
                val buffer = ShortArray(size)
                var count = 0
                var sum = 0f
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    var squares = 0.0
                    for (i in 0 until read) squares += buffer[i].toDouble() * buffer[i].toDouble()
                    val rms = sqrt(squares / read)
                    // Nilai 0 sampai sekitar 90, dihitung dari besar gelombang suara
                    val db = if (rms < 1.0) 0f else (20.0 * log10(rms)).toFloat()
                    count++
                    sum += db
                    current = db
                    average = sum / count
                    if (count == 1 || db < lowest) lowest = db
                    if (db > highest) highest = db
                }
            } catch (e: SecurityException) {
                // Izin mikrofon dicabut saat layar terbuka
            } finally {
                try {
                    recorder?.stop()
                } catch (e: Exception) {
                    // Belum sempat mulai merekam
                }
                recorder?.release()
            }
        }
    }

    ToolScaffold(stringResource(R.string.tool_decibel), onBack) {
        Column(
            modifier = Modifier.weight(1f).navigationBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!granted) {
                Text(stringResource(R.string.db_need_permission), color = Ink, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { ask.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(stringResource(R.string.db_allow), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(Modifier.height(30.dp))
                Text(text = "%.1f".format(current), color = Ink, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                Text(text = "dB", color = Muted, fontSize = 20.sp)
                Spacer(Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = { (current / 90f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Ink,
                    trackColor = LineColor,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.db_stats, lowest, average, highest),
                    color = Ink,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.db_note), color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}
