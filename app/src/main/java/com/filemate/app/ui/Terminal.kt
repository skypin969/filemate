package com.filemate.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import com.filemate.app.data.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Terminal yang dibuka dari menu. [command] diisi jika ada perintah yang langsung dijalankan (misalnya skrip). */
data class TerminalTarget(val cwd: String, val command: String?, val root: Boolean)

private const val PWD_MARK = "__FILEMATE_PWD__"
private const val MAX_LINES = 3000

private val TermBg = Color(0xFF101418)
private val TermText = Color(0xFFE6E6E6)
private val TermPrompt = Color(0xFF6FCF97)

/**
 * Terminal sederhana: satu perintah dijalankan sampai selesai, lalu hasilnya ditampilkan.
 * Folder kerja diingat, jadi `cd` berlaku untuk perintah berikutnya.
 * Program yang menunggu ketikan (misalnya `top` atau `vi`) tidak didukung.
 */
@Composable
fun TerminalScreen(target: TerminalTarget, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val lines = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var cwd by remember { mutableStateOf(target.cwd) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    // Proses yang sedang berjalan, supaya bisa dihentikan saat layar ditutup
    val running = remember { arrayOfNulls<Process>(1) }

    fun run(command: String) {
        if (command.isBlank() || busy) return
        busy = true
        lines.add((if (target.root) "# " else "$ ") + command)
        scope.launch {
            val output = withContext(Dispatchers.IO) {
                try {
                    val script = "cd ${RootShell.quote(cwd)} 2>/dev/null\n$command\nprintf '\\n$PWD_MARK%s' \"\$PWD\""
                    val process = ProcessBuilder(if (target.root) "su" else "sh", "-c", script)
                        .redirectErrorStream(true)
                        .start()
                    running[0] = process
                    val text = process.inputStream.bufferedReader().use { it.readText() }
                    process.waitFor()
                    text
                } catch (e: Exception) {
                    e.message ?: "?"
                } finally {
                    running[0] = null
                }
            }
            val mark = output.lastIndexOf(PWD_MARK)
            val shown = if (mark >= 0) output.substring(0, mark) else output
            if (mark >= 0) {
                val newDir = output.substring(mark + PWD_MARK.length).trim()
                if (newDir.isNotEmpty()) cwd = newDir
            }
            if (shown.isNotBlank()) {
                shown.trimEnd('\n').lines().forEach { lines.add(it) }
            }
            if (lines.size > MAX_LINES) lines.removeRange(0, lines.size - MAX_LINES)
            busy = false
        }
    }

    BackHandler { onBack() }

    DisposableEffect(Unit) {
        onDispose { running[0]?.destroy() }
    }

    LaunchedEffect(Unit) {
        val first = target.command
        if (first != null) run(first)
    }

    // Selalu gulir ke baris terakhir
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBg)
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBrush)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
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
                text = stringResource(R.string.tool_terminal),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            items(lines) { line ->
                Text(
                    text = line,
                    color = if (line.startsWith("$ ") || line.startsWith("# ")) TermPrompt else TermText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        Text(
            text = cwd,
            color = TermPrompt,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (target.root) "#" else "$",
                color = TermPrompt,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = TextStyle(color = TermText, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                cursorBrush = SolidColor(TermText),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val command = input
                        input = ""
                        run(command)
                    },
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
            IconButton(
                enabled = !busy,
                onClick = {
                    val command = input
                    input = ""
                    run(command)
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.terminal_run),
                    tint = if (busy) Muted else TermText,
                )
            }
        }
    }
}
