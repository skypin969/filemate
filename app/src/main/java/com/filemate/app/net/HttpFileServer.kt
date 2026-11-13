package com.filemate.app.net

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Server HTTP kecil untuk berbagi berkas di jaringan Wi-Fi yang sama.
 *
 * Semua alamat diawali kode acak [token], jadi hanya orang yang diberi alamat lengkapnya yang bisa masuk.
 * [root] adalah folder yang boleh dijelajahi dan diunduh. Jika [uploadDir] diisi, halaman depan
 * juga menampilkan tombol kirim berkas, dan berkas kiriman disimpan di folder itu (dipakai Transfer LAN).
 */
class HttpFileServer(
    private val root: File,
    private val port: Int,
    private val token: String,
    private val uploadDir: File?,
    private val title: String,
) {
    private var socket: ServerSocket? = null
    private var workers: ExecutorService? = null

    fun start() {
        val server = ServerSocket(port)
        socket = server
        val pool = Executors.newCachedThreadPool()
        workers = pool
        Thread({
            while (!server.isClosed) {
                try {
                    val client = server.accept()
                    pool.execute { serve(client) }
                } catch (e: SocketException) {
                    break
                } catch (e: Exception) {
                    // Satu sambungan gagal tidak menghentikan server
                }
            }
        }, "filemate-http").start()
    }

    fun stop() {
        try {
            socket?.close()
        } catch (e: Exception) {
            // Sudah tertutup
        }
        workers?.shutdownNow()
        socket = null
        workers = null
    }

    private fun serve(client: Socket) {
        try {
            client.soTimeout = 60_000
            client.use {
                val input = BufferedInputStream(it.getInputStream())
                val output = it.getOutputStream()
                val first = readLine(input) ?: return
                val parts = first.split(' ')
                if (parts.size < 2) return
                var length = 0L
                while (true) {
                    val header = readLine(input) ?: return
                    if (header.isEmpty()) break
                    if (header.substringBefore(':').trim().equals("content-length", ignoreCase = true)) {
                        length = header.substringAfter(':').trim().toLongOrNull() ?: 0L
                    }
                }
                val method = parts[0].uppercase()
                val target = parts[1]
                val path = decode(target.substringBefore('?'))
                val query = target.substringAfter('?', "")

                // Tanpa kode yang benar di awal alamat, permintaan ditolak
                if (path != "/$token" && !path.startsWith("/$token/")) {
                    sendText(output, 403, "Forbidden", "403")
                    return
                }
                val rest = path.removePrefix("/$token").trimStart('/')
                when {
                    method == "PUT" && rest == "upload" && uploadDir != null ->
                        receive(input, output, query, length)
                    method == "GET" && rest.isEmpty() -> sendHtml(output, listing(root, ""))
                    method == "GET" && rest.startsWith("browse") -> browse(output, rest.removePrefix("browse").trimStart('/'))
                    else -> sendText(output, 404, "Not Found", "404")
                }
            }
        } catch (e: Exception) {
            // Sambungan putus di tengah jalan
        }
    }

    /** Menampilkan isi folder, atau mengirim berkasnya jika [relative] menunjuk ke berkas. */
    private fun browse(output: OutputStream, relative: String) {
        val file = File(root, relative).canonicalFile
        // Mencegah alamat seperti ../.. keluar dari folder yang dibagikan
        if (file.path != root.canonicalPath && !file.path.startsWith(root.canonicalPath + "/")) {
            sendText(output, 403, "Forbidden", "403")
            return
        }
        when {
            file.isDirectory -> sendHtml(output, listing(file, relative.trim('/')))
            file.isFile -> sendFile(output, file)
            else -> sendText(output, 404, "Not Found", "404")
        }
    }

    /** Menerima satu berkas kiriman (isi permintaan PUT) dan menyimpannya di folder kiriman. */
    private fun receive(input: InputStream, output: OutputStream, query: String, length: Long) {
        val dir = uploadDir ?: return
        val raw = query.split('&').firstOrNull { it.startsWith("name=") }?.removePrefix("name=").orEmpty()
        // Nama berkas dibersihkan supaya tidak bisa menunjuk ke folder lain
        val name = decode(raw).substringAfterLast('/').substringAfterLast('\\').ifBlank { "berkas" }
        dir.mkdirs()
        var dest = File(dir, name)
        var n = 2
        while (dest.exists()) {
            val dot = name.lastIndexOf('.')
            dest = File(dir, if (dot > 0) "${name.substring(0, dot)} ($n)${name.substring(dot)}" else "$name ($n)")
            n++
        }
        dest.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            var left = length
            while (left > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                if (read < 0) break
                out.write(buffer, 0, read)
                left -= read
            }
        }
        sendText(output, 200, "OK", dest.name)
    }

    private fun listing(dir: File, relative: String): String {
        val html = StringBuilder()
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        html.append("<title>").append(escape(title)).append("</title><style>")
        html.append("body{font-family:sans-serif;margin:0;color:#111}h1{background:#111;color:#fff;margin:0;padding:16px;font-size:18px}")
        html.append("p,form{padding:0 16px}a{color:#111}table{width:100%;border-collapse:collapse}")
        html.append("td{padding:10px 16px;border-bottom:1px solid #e3e3e3;font-size:14px}td.s{color:#646464;white-space:nowrap;text-align:right}")
        html.append("button{background:#111;color:#fff;border:0;padding:10px 18px;border-radius:20px;font-size:14px}")
        html.append("</style></head><body><h1>").append(escape(title)).append("</h1>")

        if (uploadDir != null && relative.isEmpty()) {
            html.append("<form onsubmit=\"return kirim()\"><p><input type=\"file\" id=\"f\" multiple></p>")
            html.append("<p><button>Kirim ke HP</button> <span id=\"s\"></span></p></form>")
            html.append("<script>function kirim(){var f=document.getElementById('f').files,s=document.getElementById('s'),i=0;")
            html.append("function next(){if(i>=f.length){s.textContent='Selesai';setTimeout(function(){location.reload()},800);return}")
            html.append("s.textContent='Mengirim '+(i+1)+'/'+f.length+' ...';")
            html.append("fetch('/").append(token).append("/upload?name='+encodeURIComponent(f[i].name),{method:'PUT',body:f[i]})")
            html.append(".then(function(){i++;next()}).catch(function(){s.textContent='Gagal mengirim'})}next();return false}</script>")
        }

        html.append("<p>/").append(escape(relative)).append("</p><table>")
        if (relative.isNotEmpty()) {
            val parent = relative.substringBeforeLast('/', "")
            html.append("<tr><td><a href=\"").append(link(parent)).append("\">..</a></td><td class=\"s\"></td></tr>")
        }
        val children = dir.listFiles().orEmpty().sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        for (child in children) {
            val childPath = if (relative.isEmpty()) child.name else "$relative/${child.name}"
            val label = if (child.isDirectory) child.name + "/" else child.name
            val info = if (child.isDirectory) "" else size(child.length()) + " &nbsp; "
            html.append("<tr><td><a href=\"").append(link(childPath)).append("\">").append(escape(label)).append("</a></td>")
            html.append("<td class=\"s\">").append(info).append(stamp.format(Date(child.lastModified()))).append("</td></tr>")
        }
        html.append("</table></body></html>")
        return html.toString()
    }

    private fun link(relative: String): String {
        val encoded = relative.split('/').filter { it.isNotEmpty() }.joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }
        return if (encoded.isEmpty()) "/$token/" else "/$token/browse/$encoded"
    }

    private fun size(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun decode(text: String): String = try {
        URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")
    } catch (e: Exception) {
        text
    }

    private fun readLine(input: InputStream): String? {
        val line = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) return if (line.size() == 0) null else line.toString("UTF-8")
            if (b == '\n'.code) break
            if (b != '\r'.code) line.write(b)
            if (line.size() > 16 * 1024) return null
        }
        return line.toString("UTF-8")
    }

    private fun head(output: OutputStream, code: Int, status: String, type: String, length: Long, extra: String = "") {
        val text = "HTTP/1.1 $code $status\r\nContent-Type: $type\r\nContent-Length: $length\r\n${extra}Connection: close\r\n\r\n"
        output.write(text.toByteArray(Charsets.UTF_8))
    }

    private fun sendText(output: OutputStream, code: Int, status: String, text: String) {
        val body = text.toByteArray(Charsets.UTF_8)
        head(output, code, status, "text/plain; charset=utf-8", body.size.toLong())
        output.write(body)
        output.flush()
    }

    private fun sendHtml(output: OutputStream, html: String) {
        val body = html.toByteArray(Charsets.UTF_8)
        head(output, 200, "OK", "text/html; charset=utf-8", body.size.toLong())
        output.write(body)
        output.flush()
    }

    private fun sendFile(output: OutputStream, file: File) {
        val name = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
        head(
            output, 200, "OK", "application/octet-stream", file.length(),
            "Content-Disposition: attachment; filename*=UTF-8''$name\r\n",
        )
        file.inputStream().use { it.copyTo(output, 64 * 1024) }
        output.flush()
    }
}
