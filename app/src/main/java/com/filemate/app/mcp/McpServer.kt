package com.filemate.app.mcp

import android.content.Context
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

/**
 * Server MCP kecil di atas HTTP ("Streamable HTTP"): aplikasi AI mengirim pesan JSON-RPC
 * lewat POST ke /mcp, dan jawabannya dikirim balik sebagai JSON biasa.
 *
 * [lan] false berarti server hanya bisa dihubungi dari HP ini sendiri (127.0.0.1).
 * [onCall] dipanggil setiap satu alat selesai dijalankan, untuk mengisi Riwayat.
 */
class McpServer(
    private val context: Context,
    private val port: Int,
    private val lan: Boolean,
    private val onCall: (tool: String, millis: Long, ok: Boolean) -> Unit,
) {
    private var socket: ServerSocket? = null
    private var workers: ExecutorService? = null

    /** Membuka port dan mulai menerima sambungan. Melempar kesalahan jika port sedang dipakai. */
    fun start() {
        val address = if (lan) InetAddress.getByName("0.0.0.0") else InetAddress.getByName("127.0.0.1")
        val server = ServerSocket(port, 50, address)
        socket = server
        val pool = Executors.newCachedThreadPool()
        workers = pool
        Thread({
            while (!server.isClosed) {
                try {
                    val client = server.accept()
                    pool.execute { serve(client) }
                } catch (e: SocketException) {
                    // Server ditutup
                    break
                } catch (e: Exception) {
                    // Satu sambungan gagal tidak boleh menghentikan server
                }
            }
        }, "filemate-mcp").start()
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

    // ---------- HTTP ----------

    private class Request(val method: String, val path: String, val body: ByteArray)

    private fun serve(client: Socket) {
        try {
            client.soTimeout = 30_000
            client.use {
                val input = BufferedInputStream(it.getInputStream())
                val request = readRequest(input) ?: return
                val output = it.getOutputStream()
                val path = request.path.substringBefore('?').trimEnd('/')
                when {
                    request.method == "OPTIONS" -> respond(output, 204, "No Content", null)
                    path != "/mcp" -> respond(output, 404, "Not Found", error(null, -32601, "Use the /mcp endpoint"))
                    request.method == "POST" -> {
                        val reply = handleBody(String(request.body, Charsets.UTF_8))
                        if (reply == null) respond(output, 202, "Accepted", null) else respond(output, 200, "OK", reply)
                    }
                    request.method == "DELETE" -> respond(output, 200, "OK", null)
                    // Server ini tidak membuka aliran SSE, sesuai aturan protokol dijawab 405
                    else -> respond(output, 405, "Method Not Allowed", null)
                }
            }
        } catch (e: Exception) {
            // Sambungan putus di tengah jalan
        }
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

    private fun readRequest(input: InputStream): Request? {
        val first = readLine(input) ?: return null
        val parts = first.split(' ')
        if (parts.size < 2) return null
        var length = 0
        var chunked = false
        while (true) {
            val header = readLine(input) ?: return null
            if (header.isEmpty()) break
            val name = header.substringBefore(':').trim().lowercase()
            val value = header.substringAfter(':', "").trim()
            if (name == "content-length") length = value.toIntOrNull() ?: 0
            if (name == "transfer-encoding" && value.lowercase().contains("chunked")) chunked = true
        }
        // Batas 8 MB supaya permintaan raksasa tidak menghabiskan memori
        val max = 8 * 1024 * 1024
        val body = ByteArrayOutputStream()
        if (chunked) {
            while (true) {
                val size = readLine(input)?.substringBefore(';')?.trim()?.toIntOrNull(16) ?: return null
                if (size == 0) break
                if (body.size() + size > max) return null
                body.write(readExact(input, size) ?: return null)
                readLine(input)
            }
        } else if (length > 0) {
            if (length > max) return null
            body.write(readExact(input, length) ?: return null)
        }
        return Request(parts[0].uppercase(), parts[1], body.toByteArray())
    }

    private fun readExact(input: InputStream, count: Int): ByteArray? {
        val buffer = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val read = input.read(buffer, filled, count - filled)
            if (read < 0) return null
            filled += read
        }
        return buffer
    }

    private fun respond(output: java.io.OutputStream, code: Int, status: String, json: String?) {
        val body = json?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val head = StringBuilder()
            .append("HTTP/1.1 $code $status\r\n")
            .append("Access-Control-Allow-Origin: *\r\n")
            .append("Access-Control-Allow-Headers: *\r\n")
            .append("Access-Control-Allow-Methods: POST, GET, DELETE, OPTIONS\r\n")
            .append("Mcp-Session-Id: $sessionId\r\n")
            .append("Connection: close\r\n")
        if (json != null) head.append("Content-Type: application/json\r\n")
        head.append("Content-Length: ${body.size}\r\n\r\n")
        output.write(head.toString().toByteArray(Charsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    // ---------- JSON-RPC / MCP ----------

    private val sessionId = UUID.randomUUID().toString()

    /** Mengolah isi POST. Null berarti tidak ada yang perlu dijawab (hanya pemberitahuan). */
    private fun handleBody(text: String): String? {
        val trimmed = text.trim()
        return try {
            if (trimmed.startsWith("[")) {
                val batch = JSONArray(trimmed)
                val replies = JSONArray()
                for (i in 0 until batch.length()) {
                    handleMessage(batch.getJSONObject(i))?.let { replies.put(it) }
                }
                if (replies.length() == 0) null else replies.toString()
            } else {
                handleMessage(JSONObject(trimmed))?.toString()
            }
        } catch (e: Exception) {
            error(null, -32700, "Parse error")
        }
    }

    private fun handleMessage(message: JSONObject): JSONObject? {
        // Pesan tanpa "id" adalah pemberitahuan dan tidak dijawab
        if (!message.has("id") || message.isNull("id")) return null
        val id = message.get("id")
        val params = message.optJSONObject("params") ?: JSONObject()
        return when (val method = message.optString("method")) {
            "initialize" -> result(
                id,
                JSONObject()
                    .put("protocolVersion", params.optString("protocolVersion", "2025-03-26"))
                    .put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)))
                    .put("serverInfo", JSONObject().put("name", "Filemate MCP").put("version", "1.0"))
                    .put(
                        "instructions",
                        "Tools for working with files, installed apps and APK files on this Android phone. " +
                            "Paths are absolute; internal storage is /storage/emulated/0.",
                    ),
            )

            "ping" -> result(id, JSONObject())

            "tools/list" -> {
                val tools = JSONArray()
                McpTools.all.forEach { tools.put(McpTools.schema(it)) }
                result(id, JSONObject().put("tools", tools))
            }

            "tools/call" -> result(id, callTool(params.optString("name"), params.optJSONObject("arguments") ?: JSONObject()))

            else -> JSONObject(error(id, -32601, "Method not found: $method"))
        }
    }

    /** Menjalankan satu alat. Kegagalan alat dikirim sebagai hasil dengan isError, bukan kesalahan protokol. */
    private fun callTool(name: String, args: JSONObject): JSONObject {
        val tool = McpTools.find(name) ?: return toolResult("Unknown tool: $name", isError = true)
        val started = System.currentTimeMillis()
        var ok = false
        val reply = try {
            val text = tool.run(context, args)
            ok = true
            toolResult(text, isError = false)
        } catch (e: McpToolError) {
            toolResult(e.message ?: "Failed", isError = true)
        } catch (e: Throwable) {
            toolResult("Failed: ${e.javaClass.simpleName}: ${e.message ?: ""}", isError = true)
        }
        onCall(name, System.currentTimeMillis() - started, ok)
        return reply
    }

    private fun toolResult(text: String, isError: Boolean): JSONObject = JSONObject()
        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", text)))
        .put("isError", isError)

    private fun result(id: Any, value: JSONObject): JSONObject =
        JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", value)

    private fun error(id: Any?, code: Int, message: String): String = JSONObject()
        .put("jsonrpc", "2.0")
        .put("id", id ?: JSONObject.NULL)
        .put("error", JSONObject().put("code", code).put("message", message))
        .toString()
}
