package com.filemate.app.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.filemate.app.R
import com.filemate.app.data.FileRepository
import java.io.File

/** Berkas yang dibuka sebagai pratinjau. [markdown] true untuk berkas MD, false untuk HTML. */
data class PreviewTarget(val path: String, val markdown: Boolean)

/**
 * Pratinjau berkas HTML atau Markdown. Tombol pensil di kanan atas membuka berkasnya di editor teks.
 * Skrip di dalam halaman tidak dijalankan, supaya berkas dari sumber tak dikenal tetap aman dibuka.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(target: PreviewTarget, onBack: () -> Unit, onEdit: () -> Unit) {
    BackHandler { onBack() }
    Column(modifier = Modifier.fillMaxSize().background(Body)) {
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
            Text(
                text = FileRepository.nameOf(target.path),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.menu_edit_text),
                    tint = Color.White,
                )
            }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    val file = File(target.path)
                    val text = try {
                        file.readText()
                    } catch (e: Exception) {
                        ""
                    }
                    val html = if (target.markdown) markdownToHtml(text) else text
                    // Alamat dasar diisi folder berkasnya, supaya gambar dengan alamat relatif ikut tampil
                    loadDataWithBaseURL("file://" + file.parent + "/", html, "text/html", "UTF-8", null)
                }
            },
        )
        Spacer(Modifier.height(0.dp))
    }
}

private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** Tanda di dalam satu baris: kode, tebal, miring, dan tautan. */
private fun inlineMarkdown(line: String): String {
    var out = escapeHtml(line)
    out = Regex("`([^`]+)`").replace(out) { "<code>${it.groupValues[1]}</code>" }
    out = Regex("!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)").replace(out) {
        "<img alt=\"${it.groupValues[1]}\" src=\"${it.groupValues[2]}\">"
    }
    out = Regex("\\[([^\\]]+)\\]\\(([^)\\s]+)\\)").replace(out) {
        "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>"
    }
    out = Regex("\\*\\*([^*]+)\\*\\*").replace(out) { "<b>${it.groupValues[1]}</b>" }
    out = Regex("__([^_]+)__").replace(out) { "<b>${it.groupValues[1]}</b>" }
    out = Regex("\\*([^*]+)\\*").replace(out) { "<i>${it.groupValues[1]}</i>" }
    out = Regex("~~([^~]+)~~").replace(out) { "<s>${it.groupValues[1]}</s>" }
    return out
}

/**
 * Pengubah Markdown sederhana ke HTML: judul, paragraf, daftar, kutipan, blok kode, garis, tebal,
 * miring, kode, tautan, dan gambar. Tabel dan bentuk lanjutan lain ditampilkan sebagai teks biasa.
 */
fun markdownToHtml(source: String): String {
    val html = StringBuilder()
    html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
    html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>")
    html.append("body{font-family:sans-serif;color:#111;line-height:1.55;padding:14px;word-wrap:break-word}")
    html.append("h1,h2{border-bottom:1px solid #e3e3e3;padding-bottom:6px}")
    html.append("code{background:#f1f1f1;padding:1px 5px;border-radius:4px;font-size:90%}")
    html.append("pre{background:#f5f5f5;padding:12px;border-radius:8px;overflow-x:auto}pre code{background:none;padding:0}")
    html.append("blockquote{border-left:4px solid #c8c8c8;margin:8px 0;padding:2px 12px;color:#646464}")
    html.append("img{max-width:100%}a{color:#1565c0}hr{border:0;border-top:1px solid #e3e3e3}")
    html.append("</style></head><body>")

    var inCode = false
    var list = "" // "ul", "ol", atau kosong jika tidak sedang di dalam daftar
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            html.append("<p>").append(paragraph.toString().trim()).append("</p>")
            paragraph.setLength(0)
        }
    }

    fun closeList() {
        if (list.isNotEmpty()) {
            html.append("</").append(list).append(">")
            list = ""
        }
    }

    for (raw in source.lines()) {
        val line = raw.trimEnd()
        if (line.trimStart().startsWith("```")) {
            flushParagraph()
            closeList()
            html.append(if (inCode) "</code></pre>" else "<pre><code>")
            inCode = !inCode
            continue
        }
        if (inCode) {
            html.append(escapeHtml(raw)).append('\n')
            continue
        }
        val trimmed = line.trim()
        val heading = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
        val bullet = Regex("^[-*+]\\s+(.*)$").find(trimmed)
        val numbered = Regex("^\\d+[.)]\\s+(.*)$").find(trimmed)
        when {
            trimmed.isEmpty() -> {
                flushParagraph()
                closeList()
            }
            heading != null -> {
                flushParagraph()
                closeList()
                val level = heading.groupValues[1].length
                html.append("<h$level>").append(inlineMarkdown(heading.groupValues[2])).append("</h$level>")
            }
            trimmed.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$")) -> {
                flushParagraph()
                closeList()
                html.append("<hr>")
            }
            trimmed.startsWith(">") -> {
                flushParagraph()
                closeList()
                html.append("<blockquote>").append(inlineMarkdown(trimmed.removePrefix(">").trim())).append("</blockquote>")
            }
            bullet != null || numbered != null -> {
                flushParagraph()
                val kind = if (bullet != null) "ul" else "ol"
                if (list != kind) {
                    closeList()
                    html.append("<").append(kind).append(">")
                    list = kind
                }
                val content = (bullet ?: numbered)?.groupValues?.get(1).orEmpty()
                html.append("<li>").append(inlineMarkdown(content)).append("</li>")
            }
            else -> {
                closeList()
                paragraph.append(inlineMarkdown(trimmed)).append(' ')
            }
        }
    }
    flushParagraph()
    closeList()
    if (inCode) html.append("</code></pre>")
    html.append("</body></html>")
    return html.toString()
}
