package com.filemate.app.mcp

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.filemate.app.apk.ApkTools
import com.filemate.app.data.FileRepository
import com.filemate.app.data.Trash
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile
import org.json.JSONArray
import org.json.JSONObject

/** Satu parameter alat: nama, jenis ("string", "integer", "boolean"), keterangan, dan wajib atau tidak. */
class McpParam(val name: String, val type: String, val description: String, val required: Boolean = true)

/**
 * Satu alat yang bisa dipanggil AI lewat layanan MCP.
 * [write] true berarti alat ini mengubah berkas di HP. [group] dipakai halaman bantuan untuk mengelompokkan.
 */
class McpTool(
    val name: String,
    val group: String,
    val description: String,
    val write: Boolean,
    val params: List<McpParam>,
    val run: (Context, JSONObject) -> String,
)

/** Kesalahan yang pesannya dikirim apa adanya ke AI, misalnya "berkas tidak ditemukan". */
class McpToolError(message: String) : Exception(message)

/**
 * Daftar alat MCP Filemate. Namanya mengikuti pola NP Manager (np_...) dengan awalan fm_.
 * Tahap awal: berkas dan sistem, aplikasi terpasang, dan alat APK yang mesinnya sudah ada di Filemate.
 */
object McpTools {

    const val GROUP_FILES = "files"
    const val GROUP_OPEN = "open"
    const val GROUP_VIEW = "view"
    const val GROUP_BUILD = "build"

    private const val STORAGE = FileRepository.STORAGE_ROOT
    private const val MAX_TEXT = 64 * 1024

    val all: List<McpTool> = listOf(
        // ---------- Berkas dan sistem ----------
        McpTool(
            "fm_list_directory", GROUP_FILES,
            "List the files and folders inside a directory on the phone. Returns name, type, size and modified time.",
            write = false,
            params = listOf(McpParam("path", "string", "Absolute directory path, e.g. /storage/emulated/0/Download")),
        ) { _, a ->
            val dir = existing(a.path("path"))
            if (!dir.isDirectory) fail("Not a directory: ${dir.path}")
            val children = dir.listFiles() ?: fail("Cannot read directory: ${dir.path}")
            val list = JSONArray()
            children.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })).forEach { list.put(describe(it)) }
            JSONObject().put("path", dir.path).put("count", list.length()).put("entries", list).toString(2)
        },
        McpTool(
            "fm_get_file_info", GROUP_FILES,
            "Get details of one file or folder: type, size in bytes, modified time, and whether it is readable and writable.",
            write = false,
            params = listOf(McpParam("path", "string", "Absolute path of the file or folder")),
        ) { _, a ->
            val file = existing(a.path("path"))
            describe(file).put("path", file.path).put("readable", file.canRead()).put("writable", file.canWrite()).toString(2)
        },
        McpTool(
            "fm_read_file", GROUP_FILES,
            "Read a text file. Returns at most 64 KB per call; use offset to continue reading a longer file.",
            write = false,
            params = listOf(
                McpParam("path", "string", "Absolute path of the text file"),
                McpParam("offset", "integer", "Byte position to start from (default 0)", required = false),
            ),
        ) { _, a ->
            val file = existing(a.path("path"))
            if (file.isDirectory) fail("This is a directory, use fm_list_directory: ${file.path}")
            val offset = a.optLong("offset", 0L).coerceAtLeast(0L)
            val bytes = file.inputStream().use { input ->
                input.skip(offset)
                val buffer = ByteArray(MAX_TEXT)
                val read = input.read(buffer).coerceAtLeast(0)
                buffer.copyOf(read)
            }
            val next = offset + bytes.size
            val more = if (next < file.length()) "\n\n[truncated: file is ${file.length()} bytes, continue with offset=$next]" else ""
            String(bytes, Charsets.UTF_8) + more
        },
        McpTool(
            "fm_search_text", GROUP_FILES,
            "Search for a piece of text inside the text files of a folder (including subfolders). Returns matching file paths with line numbers.",
            write = false,
            params = listOf(
                McpParam("path", "string", "Absolute path of the folder to search in"),
                McpParam("query", "string", "Text to look for (case-insensitive)"),
                McpParam("max_results", "integer", "Maximum number of matches to return (default 50)", required = false),
            ),
        ) { _, a ->
            val root = existing(a.path("path"))
            val query = a.optString("query")
            if (query.isEmpty()) fail("query is empty")
            val limit = a.optInt("max_results", 50).coerceIn(1, 500)
            val hits = JSONArray()
            var scanned = 0
            for (file in root.walkTopDown().maxDepth(12)) {
                if (hits.length() >= limit || scanned >= 5000) break
                if (!file.isFile || file.length() > 1024 * 1024) continue
                scanned++
                try {
                    file.useLines { lines ->
                        var number = 0
                        for (line in lines) {
                            number++
                            if (line.contains(query, ignoreCase = true)) {
                                hits.put(JSONObject().put("file", file.path).put("line", number).put("text", line.take(200)))
                                if (hits.length() >= limit) break
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Berkas biner atau tidak terbaca dilewati
                }
            }
            JSONObject().put("matches", hits).put("files_scanned", scanned).toString(2)
        },
        McpTool(
            "fm_write_file", GROUP_FILES,
            "Create or overwrite a text file inside internal storage with the given content.",
            write = true,
            params = listOf(
                McpParam("path", "string", "Absolute path under /storage/emulated/0"),
                McpParam("content", "string", "Full text content of the file"),
            ),
        ) { _, a ->
            val file = writable(a.path("path"))
            file.parentFile?.mkdirs()
            file.writeText(a.optString("content"), Charsets.UTF_8)
            "Saved ${file.length()} bytes to ${file.path}"
        },
        McpTool(
            "fm_make_directory", GROUP_FILES,
            "Create a folder (and any missing parent folders) inside internal storage.",
            write = true,
            params = listOf(McpParam("path", "string", "Absolute path under /storage/emulated/0")),
        ) { _, a ->
            val dir = writable(a.path("path"))
            if (!dir.isDirectory && !dir.mkdirs()) fail("Could not create folder: ${dir.path}")
            "Folder ready: ${dir.path}"
        },
        McpTool(
            "fm_copy", GROUP_FILES,
            "Copy a file or folder to a new location inside internal storage.",
            write = true,
            params = listOf(
                McpParam("from", "string", "Absolute path of the source"),
                McpParam("to", "string", "Absolute destination path under /storage/emulated/0"),
            ),
        ) { _, a ->
            val from = existing(a.path("from"))
            val to = writable(a.path("to"))
            if (to.exists()) fail("Destination already exists: ${to.path}")
            if (!from.copyRecursively(to, overwrite = false)) fail("Copy failed")
            "Copied to ${to.path}"
        },
        McpTool(
            "fm_move", GROUP_FILES,
            "Move or rename a file or folder inside internal storage.",
            write = true,
            params = listOf(
                McpParam("from", "string", "Absolute path of the source under /storage/emulated/0"),
                McpParam("to", "string", "Absolute destination path under /storage/emulated/0"),
            ),
        ) { _, a ->
            val from = existing(writable(a.path("from")).path)
            val to = writable(a.path("to"))
            if (to.exists()) fail("Destination already exists: ${to.path}")
            to.parentFile?.mkdirs()
            if (!from.renameTo(to)) fail("Move failed")
            "Moved to ${to.path}"
        },
        McpTool(
            "fm_delete", GROUP_FILES,
            "Delete a file or folder inside internal storage. It is moved to Filemate's Recycle Bin, so the user can restore it.",
            write = true,
            params = listOf(McpParam("path", "string", "Absolute path under /storage/emulated/0")),
        ) { _, a ->
            val file = existing(writable(a.path("path")).path)
            if (!Trash.supports(file.path)) fail("This path cannot be deleted through MCP: ${file.path}")
            if (!Trash.move(file.path)) fail("Delete failed")
            "Moved to Recycle Bin: ${file.path}"
        },

        // ---------- Buka aplikasi ----------
        McpTool(
            "fm_list_installed_apps", GROUP_OPEN,
            "List the apps installed on the phone with package name, label, version and whether it is a system app.",
            write = false,
            params = listOf(
                McpParam("type", "string", "\"user\" (default), \"system\" or \"all\"", required = false),
                McpParam("query", "string", "Only apps whose label or package contains this text", required = false),
            ),
        ) { context, a ->
            val type = a.optString("type", "user")
            val query = a.optString("query")
            val pm = context.packageManager
            val list = JSONArray()
            pm.getInstalledPackages(0).forEach { info ->
                val app = info.applicationInfo ?: return@forEach
                val system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (type == "user" && system) return@forEach
                if (type == "system" && !system) return@forEach
                val label = app.loadLabel(pm).toString()
                if (query.isNotEmpty() && !label.contains(query, true) && !info.packageName.contains(query, true)) return@forEach
                list.put(
                    JSONObject()
                        .put("package", info.packageName)
                        .put("label", label)
                        .put("version", info.versionName ?: "")
                        .put("system", system)
                        .put("apk_path", app.sourceDir ?: ""),
                )
            }
            JSONObject().put("count", list.length()).put("apps", list).toString(2)
        },
        McpTool(
            "fm_device_pull_apk", GROUP_OPEN,
            "Copy the APK of an installed app into /storage/emulated/0/Filemate/apk so it can be inspected. First step for studying an installed app.",
            write = true,
            params = listOf(McpParam("package", "string", "Package name, e.g. com.example.app")),
        ) { context, a ->
            val name = a.optString("package")
            val app = try {
                context.packageManager.getApplicationInfo(name, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                fail("App is not installed: $name")
            }
            val dir = File(FileRepository.FILEMATE_DIR, "apk")
            dir.mkdirs()
            val dest = File(dir, "$name.apk")
            File(app.sourceDir).copyTo(dest, overwrite = true)
            val splits = app.splitSourceDirs.orEmpty().size
            val note = if (splits > 0) " Note: this app also has $splits split APK(s) that were not copied." else ""
            "Saved ${dest.length()} bytes to ${dest.path}.$note"
        },
        McpTool(
            "fm_apk_list_available_apks", GROUP_OPEN,
            "Find APK files in a folder (default: Download and Filemate folders), including subfolders.",
            write = false,
            params = listOf(McpParam("path", "string", "Folder to search in (optional)", required = false)),
        ) { _, a ->
            val roots = if (a.optString("path").isNotEmpty()) {
                listOf(existing(a.path("path")))
            } else {
                listOf(File(STORAGE, "Download"), File(FileRepository.FILEMATE_DIR))
            }
            val list = JSONArray()
            roots.filter { it.isDirectory }.forEach { root ->
                root.walkTopDown().maxDepth(4).filter { it.isFile && it.name.endsWith(".apk", true) }.take(300).forEach {
                    list.put(describe(it).put("path", it.path))
                }
            }
            JSONObject().put("count", list.length()).put("apks", list).toString(2)
        },

        // ---------- Lihat dan analisis ----------
        McpTool(
            "fm_get_apk_info", GROUP_VIEW,
            "Read basic information from an APK file: package, version, SDK levels, permissions and components (activities, services, receivers, providers).",
            write = false,
            params = listOf(McpParam("path", "string", "Absolute path of the .apk file")),
        ) { context, a ->
            val file = existing(a.path("path"))
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
            val info = context.packageManager.getPackageArchiveInfo(file.path, flags)
                ?: fail("Not a readable APK: ${file.path}")
            val app = info.applicationInfo
            JSONObject()
                .put("package", info.packageName)
                .put("version_name", info.versionName ?: "")
                .put("version_code", info.longVersionCode)
                .put("min_sdk", app?.minSdkVersion ?: 0)
                .put("target_sdk", app?.targetSdkVersion ?: 0)
                .put("size_bytes", file.length())
                .put("permissions", JSONArray(info.requestedPermissions?.toList().orEmpty()))
                .put("activities", JSONArray(info.activities?.map { it.name }.orEmpty()))
                .put("services", JSONArray(info.services?.map { it.name }.orEmpty()))
                .put("receivers", JSONArray(info.receivers?.map { it.name }.orEmpty()))
                .put("providers", JSONArray(info.providers?.map { it.name }.orEmpty()))
                .toString(2)
        },
        McpTool(
            "fm_apk_read_manifest", GROUP_VIEW,
            "Decode and return the AndroidManifest.xml of an APK as readable text.",
            write = false,
            params = listOf(McpParam("path", "string", "Absolute path of the .apk file")),
        ) { _, a ->
            ApkTools.readManifest(existing(a.path("path"))).take(MAX_TEXT * 2)
        },
        McpTool(
            "fm_apk_list_entries", GROUP_VIEW,
            "List the files packed inside an APK (or any ZIP) with their sizes. Use prefix to look at one folder, e.g. res/ or lib/.",
            write = false,
            params = listOf(
                McpParam("path", "string", "Absolute path of the .apk file"),
                McpParam("prefix", "string", "Only entries whose name starts with this text", required = false),
            ),
        ) { _, a ->
            val file = existing(a.path("path"))
            val prefix = a.optString("prefix")
            val list = JSONArray()
            var total = 0
            ZipFile(file).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.isDirectory || !entry.name.startsWith(prefix)) continue
                    total++
                    if (list.length() < 1000) list.put(JSONObject().put("name", entry.name).put("size", entry.size))
                }
            }
            JSONObject().put("total", total).put("shown", list.length()).put("entries", list).toString(2)
        },
        McpTool(
            "fm_apk_read_entry", GROUP_VIEW,
            "Read one plain-text file packed inside an APK (for example a file under assets/). Binary files such as compiled XML, .dex and .arsc are not decoded yet.",
            write = false,
            params = listOf(
                McpParam("path", "string", "Absolute path of the .apk file"),
                McpParam("entry", "string", "Entry name inside the APK, e.g. assets/config.json"),
            ),
        ) { _, a ->
            val file = existing(a.path("path"))
            val name = a.optString("entry")
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(name) ?: fail("No such entry: $name")
                val bytes = zip.getInputStream(entry).use { input ->
                    val buffer = ByteArray(MAX_TEXT)
                    var filled = 0
                    while (filled < buffer.size) {
                        val read = input.read(buffer, filled, buffer.size - filled)
                        if (read < 0) break
                        filled += read
                    }
                    buffer.copyOf(filled)
                }
                // Byte nol menandakan berkas biner
                if (bytes.any { it.toInt() == 0 }) {
                    fail("$name is a binary file (${entry.size} bytes). Decoding this type is not available yet.")
                }
                val more = if (entry.size > bytes.size) "\n\n[truncated: entry is ${entry.size} bytes]" else ""
                String(bytes, Charsets.UTF_8) + more
            }
        },

        // ---------- Rakit dan tanda tangan ----------
        McpTool(
            "fm_apk_unpack", GROUP_BUILD,
            "Unpack an APK into a working folder at /storage/emulated/0/Filemate/<name>. The files can then be edited and repacked.",
            write = true,
            params = listOf(McpParam("path", "string", "Absolute path of the .apk file")),
        ) { _, a ->
            val file = existing(a.path("path"))
            val out = File(FileRepository.FILEMATE_DIR, file.nameWithoutExtension)
            ApkTools.unpack(file, out)
            "Unpacked to ${out.path}"
        },
        McpTool(
            "fm_apk_repack", GROUP_BUILD,
            "Pack a working folder (made by fm_apk_unpack) back into an unsigned APK next to it. Sign it with fm_apk_sign before installing.",
            write = true,
            params = listOf(McpParam("path", "string", "Absolute path of the unpacked folder")),
        ) { _, a ->
            val dir = existing(writable(a.path("path")).path)
            if (!dir.isDirectory) fail("Not a folder: ${dir.path}")
            val out = File(dir.parentFile, dir.name + "-rakit.apk")
            ApkTools.repack(dir, out)
            "Repacked to ${out.path} (${out.length()} bytes, unsigned)"
        },
        McpTool(
            "fm_apk_sign", GROUP_BUILD,
            "Sign an APK with Filemate's own test key so Android accepts it. The result is saved next to the input with -signed in its name.",
            write = true,
            params = listOf(McpParam("path", "string", "Absolute path of the .apk file to sign")),
        ) { _, a ->
            val file = existing(a.path("path"))
            val out = writable(File(file.parentFile, file.nameWithoutExtension + "-signed.apk").path)
            ApkTools.sign(file, out)
            "Signed APK saved to ${out.path} (${out.length()} bytes)"
        },
    )

    fun find(name: String): McpTool? = all.firstOrNull { it.name == name }

    /** Bentuk JSON satu alat untuk jawaban "tools/list". */
    fun schema(tool: McpTool): JSONObject {
        val props = JSONObject()
        val required = JSONArray()
        tool.params.forEach { p ->
            props.put(p.name, JSONObject().put("type", p.type).put("description", p.description))
            if (p.required) required.put(p.name)
        }
        return JSONObject()
            .put("name", tool.name)
            .put("description", tool.description)
            .put("inputSchema", JSONObject().put("type", "object").put("properties", props).put("required", required))
            .put("annotations", JSONObject().put("readOnlyHint", !tool.write))
    }

    // ---------- Alat bantu ----------

    private fun fail(message: String): Nothing = throw McpToolError(message)

    /** Mengambil parameter jalur. "/sdcard" disamakan dengan penyimpanan internal. */
    private fun JSONObject.path(key: String): String {
        val raw = optString(key).trim()
        if (raw.isEmpty()) fail("Missing parameter: $key")
        if (!raw.startsWith("/")) fail("$key must be an absolute path starting with /")
        return if (raw == "/sdcard" || raw.startsWith("/sdcard/")) STORAGE + raw.removePrefix("/sdcard") else raw
    }

    private fun existing(path: String): File {
        val file = File(path)
        if (!file.exists()) fail("Not found: $path")
        return file
    }

    /** Alat yang mengubah berkas hanya boleh bekerja di penyimpanan internal, bukan di folder sistem. */
    private fun writable(path: String): File {
        val file = File(path).canonicalFile
        if (!file.path.startsWith("$STORAGE/")) fail("Writing is only allowed under $STORAGE, not: $path")
        return file
    }

    private fun describe(file: File): JSONObject = JSONObject()
        .put("name", file.name)
        .put("type", if (file.isDirectory) "folder" else "file")
        .put("size", if (file.isDirectory) 0L else file.length())
        .put("modified", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(file.lastModified())))
}
