package com.filemate.app.net

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Server FTP sederhana (mode pasif) untuk mengakses [root] dari komputer atau HP lain di Wi-Fi yang sama.
 * Masuk memakai [user] dan [password] yang ditampilkan di dialog Layanan FTP.
 */
class FtpServer(
    private val root: File,
    private val port: Int,
    private val user: String,
    private val password: String,
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
                    pool.execute { Session(client).run() }
                } catch (e: SocketException) {
                    break
                } catch (e: Exception) {
                    // Satu sambungan gagal tidak menghentikan server
                }
            }
        }, "filemate-ftp").start()
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

    /** Satu klien FTP yang sedang tersambung. */
    private inner class Session(private val control: Socket) {
        private val out: OutputStream = control.getOutputStream()
        private var cwd = "/"
        private var loggedIn = false
        private var nameOk = false
        private var passive: ServerSocket? = null
        private var renameFrom: File? = null

        fun run() {
            try {
                control.soTimeout = 10 * 60_000
                control.use {
                    val reader = BufferedReader(InputStreamReader(it.getInputStream(), Charsets.UTF_8))
                    reply("220 Filemate FTP")
                    while (true) {
                        val line = reader.readLine() ?: break
                        val command = line.substringBefore(' ').uppercase()
                        val arg = line.substringAfter(' ', "").trim()
                        if (command == "QUIT") {
                            reply("221 Bye")
                            break
                        }
                        handle(command, arg)
                    }
                }
            } catch (e: Exception) {
                // Klien memutus sambungan
            } finally {
                closePassive()
            }
        }

        private fun reply(text: String) {
            out.write((text + "\r\n").toByteArray(Charsets.UTF_8))
            out.flush()
        }

        private fun handle(command: String, arg: String) {
            when (command) {
                "USER" -> {
                    nameOk = arg == user
                    reply("331 Password required")
                }
                "PASS" -> {
                    loggedIn = nameOk && arg == password
                    reply(if (loggedIn) "230 Logged in" else "530 Login incorrect")
                }
                "SYST" -> reply("215 UNIX Type: L8")
                "FEAT" -> {
                    reply("211-Features")
                    reply(" UTF8")
                    reply(" PASV")
                    reply(" EPSV")
                    reply(" SIZE")
                    reply(" MDTM")
                    reply("211 End")
                }
                "OPTS", "NOOP" -> reply("200 OK")
                "TYPE", "MODE", "STRU" -> reply("200 OK")
                else -> if (!loggedIn) reply("530 Please login") else handleAuthed(command, arg)
            }
        }

        private fun handleAuthed(command: String, arg: String) {
            when (command) {
                "PWD", "XPWD" -> reply("257 \"$cwd\" is current directory")
                "CWD", "XCWD" -> {
                    val target = resolve(arg)
                    if (target != null && target.isDirectory) {
                        cwd = virtual(target)
                        reply("250 OK")
                    } else {
                        reply("550 No such directory")
                    }
                }
                "CDUP", "XCUP" -> {
                    val target = resolve("..")
                    if (target != null) cwd = virtual(target)
                    reply("250 OK")
                }
                "PASV" -> {
                    closePassive()
                    val data = ServerSocket(0)
                    passive = data
                    val host = control.localAddress.hostAddress.orEmpty().replace('.', ',')
                    reply("227 Entering Passive Mode ($host,${data.localPort / 256},${data.localPort % 256})")
                }
                "EPSV" -> {
                    closePassive()
                    val data = ServerSocket(0)
                    passive = data
                    reply("229 Entering Extended Passive Mode (|||${data.localPort}|)")
                }
                "LIST", "NLST", "MLSD" -> {
                    // Sebagian klien mengirim pilihan seperti "-la" sebelum nama folder
                    val name = arg.split(' ').filterNot { it.startsWith("-") }.joinToString(" ")
                    val dir = if (name.isEmpty()) resolve(".") else resolve(name)
                    if (dir == null || !dir.exists()) {
                        reply("550 Not found")
                        return
                    }
                    val entries = if (dir.isDirectory) dir.listFiles().orEmpty().sortedBy { it.name.lowercase() } else listOf(dir)
                    transfer { stream ->
                        val text = StringBuilder()
                        for (file in entries) {
                            text.append(if (command == "NLST") file.name else listLine(file)).append("\r\n")
                        }
                        stream.getOutputStream().write(text.toString().toByteArray(Charsets.UTF_8))
                    }
                }
                "RETR" -> {
                    val file = resolve(arg)
                    if (file == null || !file.isFile) {
                        reply("550 Not found")
                        return
                    }
                    transfer { stream -> file.inputStream().use { it.copyTo(stream.getOutputStream(), 64 * 1024) } }
                }
                "STOR" -> {
                    val file = resolve(arg)
                    if (file == null) {
                        reply("553 Not allowed")
                        return
                    }
                    transfer { stream -> file.outputStream().use { stream.getInputStream().copyTo(it, 64 * 1024) } }
                }
                "SIZE" -> {
                    val file = resolve(arg)
                    if (file != null && file.isFile) reply("213 ${file.length()}") else reply("550 Not found")
                }
                "MDTM" -> {
                    val file = resolve(arg)
                    if (file != null && file.exists()) {
                        reply("213 " + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date(file.lastModified())))
                    } else {
                        reply("550 Not found")
                    }
                }
                "DELE" -> {
                    val file = resolve(arg)
                    reply(if (file != null && file.isFile && file.delete()) "250 Deleted" else "550 Delete failed")
                }
                "RMD", "XRMD" -> {
                    val file = resolve(arg)
                    reply(if (file != null && file.isDirectory && file.delete()) "250 Removed" else "550 Remove failed")
                }
                "MKD", "XMKD" -> {
                    val file = resolve(arg)
                    reply(if (file != null && file.mkdirs()) "257 \"${virtual(file)}\" created" else "550 Create failed")
                }
                "RNFR" -> {
                    val file = resolve(arg)
                    if (file != null && file.exists()) {
                        renameFrom = file
                        reply("350 Ready")
                    } else {
                        reply("550 Not found")
                    }
                }
                "RNTO" -> {
                    val from = renameFrom
                    val to = resolve(arg)
                    renameFrom = null
                    reply(if (from != null && to != null && from.renameTo(to)) "250 Renamed" else "550 Rename failed")
                }
                else -> reply("502 Command not implemented")
            }
        }

        /** Membuka sambungan data yang sudah disiapkan PASV, menjalankan [block], lalu menutupnya. */
        private fun transfer(block: (Socket) -> Unit) {
            val data = passive
            if (data == null) {
                reply("425 Use PASV first")
                return
            }
            reply("150 Opening data connection")
            try {
                data.soTimeout = 30_000
                data.accept().use { block(it) }
                reply("226 Transfer complete")
            } catch (e: Exception) {
                reply("426 Transfer failed")
            } finally {
                closePassive()
            }
        }

        private fun closePassive() {
            try {
                passive?.close()
            } catch (e: Exception) {
                // Sudah tertutup
            }
            passive = null
        }

        /** Mengubah alamat dari klien menjadi berkas asli. Null jika alamatnya keluar dari folder yang dibagikan. */
        private fun resolve(arg: String): File? {
            val base = root.canonicalFile
            val combined = if (arg.startsWith("/")) File(base, arg) else File(File(base, cwd), arg)
            val file = combined.canonicalFile
            return if (file.path == base.path || file.path.startsWith(base.path + "/")) file else null
        }

        /** Alamat berkas seperti yang dilihat klien, dengan [root] sebagai "/". */
        private fun virtual(file: File): String {
            val rest = file.canonicalPath.removePrefix(root.canonicalPath)
            return if (rest.isEmpty()) "/" else rest
        }

        private fun listLine(file: File): String {
            val type = if (file.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
            val date = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date(file.lastModified()))
            return "$type 1 filemate filemate ${file.length()} $date ${file.name}"
        }
    }
}
