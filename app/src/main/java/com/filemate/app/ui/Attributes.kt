package com.filemate.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import com.filemate.app.data.RootShell
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Izin, pemilik, dan grup satu berkas, hasil perintah `stat`. */
private class Ownership(
    val symbolic: String, // contoh: -rwxrwx---
    val octal: String,    // contoh: 770
    val owner: String,
    val group: String,
    /** Angka izin khusus: 4 setel UID, 2 setel GID, 1 lengket. Bisa dijumlah. */
    val special: Int = 0,
)

/** Isi sebuah folder: jumlah berkas, jumlah folder, dan ukuran totalnya. */
private class FolderStats(val files: Int, val folders: Int, val bytes: Long)

private fun readOwnership(path: String, root: Boolean): Ownership? {
    val result = RootShell.exec("stat -c '%A|%a|%U|%G' ${RootShell.quote(path)}", root)
    if (result.exitCode != 0) return null
    val parts = result.output.trim().lines().lastOrNull()?.split('|') ?: return null
    if (parts.size != 4) return null
    val digits = parts[1].padStart(4, '0').takeLast(4)
    return Ownership(parts[0], digits.takeLast(3), parts[2], parts[3], special = digits[0].digitToIntOrNull() ?: 0)
}

/** Menghitung isi folder. Berhenti di 50.000 item supaya tidak lama. Null jika folder tidak bisa dibaca. */
private fun readFolderStats(path: String): FolderStats? {
    val dir = File(path)
    if (!dir.canRead()) return null
    var files = 0
    var folders = 0
    var bytes = 0L
    var seen = 0
    for (entry in dir.walkTopDown()) {
        if (entry == dir) continue
        if (entry.isDirectory) {
            folders++
        } else {
            files++
            bytes += entry.length()
        }
        seen++
        if (seen >= 50_000) break
    }
    return FolderStats(files, folders, bytes)
}

/** Sidik jari berkas (MD5, SHA-1, SHA-256) untuk tombol Verifikasi. */
private fun readHashes(path: String): String? = try {
    val names = listOf("MD5", "SHA-1", "SHA-256")
    val digests = names.map { MessageDigest.getInstance(it) }
    File(path).inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digests.forEach { it.update(buffer, 0, read) }
        }
    }
    names.indices.joinToString("\n\n") { i ->
        names[i] + "\n" + digests[i].digest().joinToString("") { b -> "%02x".format(b) }
    }
} catch (e: Exception) {
    null
}

/**
 * Dialog atribut seperti NP Manager: label di kiri, isi di kanan, lengkap dengan izin,
 * pemilik, dan grup. Tombol UBAH membuka dialog izin, tombol Verifikasi menghitung sidik jari berkas.
 */
@Composable
fun AttributesDialog(
    item: FileItem,
    root: Boolean,
    onChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var owner by remember { mutableStateOf<Ownership?>(null) }
    var stats by remember { mutableStateOf<FolderStats?>(null) }
    var hashes by remember { mutableStateOf<String?>(null) }
    var hashing by remember { mutableStateOf(false) }
    var editPerm by remember { mutableStateOf(false) }
    var editOwner by remember { mutableStateOf(false) }
    // Naik setiap izin diubah, supaya izin dibaca ulang
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(item.path, reload) {
        owner = withContext(Dispatchers.IO) { readOwnership(item.path, root) }
    }
    LaunchedEffect(item.path) {
        if (item.isDir) stats = withContext(Dispatchers.IO) { readFolderStats(item.path) }
    }

    val folderStats = stats
    val size = when {
        !item.isDir -> "${formatSize(item.size)} (${item.size})"
        folderStats != null -> "${formatSize(folderStats.bytes)} (${folderStats.bytes})"
        else -> "-"
    }
    val perm = owner

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.dialog_attributes_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AttrRow(stringResource(R.string.attr_name), item.name)
                AttrRow(stringResource(R.string.attr_location), FileRepository.parentOf(item.path))
                AttrRow(
                    label = stringResource(R.string.attr_type),
                    value = stringResource(if (item.isDir) R.string.attr_type_folder else R.string.attr_type_file),
                )
                AttrRow(stringResource(R.string.attr_size), size)
                AttrRow(stringResource(R.string.attr_modified), formatDateFull(item.modified))

                Row(
                    modifier = Modifier.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.attr_permission),
                        color = Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.width(92.dp),
                    )
                    Text(
                        text = if (perm != null) "${perm.symbolic} (${perm.octal})" else "-",
                        color = Ink,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (perm != null) {
                        Text(
                            text = stringResource(R.string.attr_change),
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { editPerm = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        AttrRow(stringResource(R.string.attr_owner), perm?.owner ?: "-")
                        AttrRow(stringResource(R.string.attr_group), perm?.group ?: "-")
                    }
                    // Mengganti pemilik dan grup butuh akses root
                    if (perm != null && root) {
                        Text(
                            text = stringResource(R.string.attr_change),
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { editOwner = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (item.isDir && folderStats != null) {
                    AttrRow(stringResource(R.string.attr_file_count), folderStats.files.toString())
                    AttrRow(stringResource(R.string.attr_folder_count), folderStats.folders.toString())
                }

                val shownHashes = hashes
                if (hashing) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.attr_hashing), color = Muted, fontSize = 13.sp)
                } else if (shownHashes != null) {
                    Spacer(Modifier.height(8.dp))
                    // Teks bisa diblok dan disalin
                    SelectionContainer {
                        Text(
                            text = shownHashes,
                            color = Ink,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            if (!item.isDir) {
                TextButton(
                    enabled = !hashing,
                    onClick = {
                        hashing = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { readHashes(item.path) }
                            hashes = result ?: "-"
                            hashing = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.attr_verify), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    )

    if (editPerm && perm != null) {
        PermissionDialog(
            item = item,
            current = perm,
            onApply = { octal, toFiles, toFolders ->
                editPerm = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val path = RootShell.quote(item.path)
                        RootShell.exec("chmod $octal $path", root)
                        // "Juga berlaku untuk semua file / folder" di dalamnya
                        if (toFiles) RootShell.exec("find $path -type f -exec chmod $octal {} +", root)
                        if (toFolders) RootShell.exec("find $path -mindepth 1 -type d -exec chmod $octal {} +", root)
                    }
                    reload++
                    onChanged()
                }
            },
            onDismiss = { editPerm = false },
        )
    }

    if (editOwner && perm != null) {
        OwnerDialog(
            item = item,
            current = perm,
            onApply = { newOwner, newGroup, toFiles, toFolders ->
                editOwner = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val path = RootShell.quote(item.path)
                        val who = "$newOwner:$newGroup"
                        RootShell.exec("chown $who $path", root)
                        if (toFiles) RootShell.exec("find $path -type f -exec chown $who {} +", root)
                        if (toFolders) RootShell.exec("find $path -mindepth 1 -type d -exec chown $who {} +", root)
                    }
                    reload++
                    onChanged()
                }
            },
            onDismiss = { editOwner = false },
        )
    }
}

@Composable
private fun AttrRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 7.dp)) {
        Text(
            text = label,
            color = Muted,
            fontSize = 14.sp,
            modifier = Modifier.width(92.dp),
        )
        Text(text = value, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

/**
 * Dialog izin: tiga kolom (pemilik, grup, lainnya) dan tiga baris centang (baca, tulis, jalankan).
 * Angka izin di bawahnya ikut berubah saat centang diubah.
 */
@Composable
private fun PermissionDialog(
    item: FileItem,
    current: Ownership,
    onApply: (String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    // Sembilan bit izin, urutannya: pemilik baca/tulis/jalankan, grup, lalu lainnya
    val start = current.octal.map { ch -> ch.digitToIntOrNull() ?: 0 }
    var bits by remember {
        mutableStateOf(
            List(9) { i -> ((start.getOrElse(i / 3) { 0 } shr (2 - i % 3)) and 1) == 1 },
        )
    }
    // Izin khusus: setel UID (4), setel GID (2), lengket (1)
    var special by remember { mutableStateOf(current.special) }
    var toFiles by remember { mutableStateOf(false) }
    var toFolders by remember { mutableStateOf(false) }
    // Untuk folder sistem, tombol OK baru aktif setelah user mencentang bahwa dia paham risikonya
    val system = FileRepository.isSystemPath(item.path)
    var understood by remember { mutableStateOf(false) }
    val octal = special.toString() + (0 until 3).joinToString("") { who ->
        var digit = 0
        if (bits[who * 3]) digit += 4
        if (bits[who * 3 + 1]) digit += 2
        if (bits[who * 3 + 2]) digit += 1
        digit.toString()
    }
    val columns = listOf(R.string.attr_owner, R.string.attr_group, R.string.perm_others)
    val rows = listOf(R.string.perm_read, R.string.perm_write, R.string.perm_execute)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.attr_permission), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    columns.forEach { title ->
                        Text(
                            text = stringResource(title),
                            color = Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                rows.forEachIndexed { what, label ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        (0 until 3).forEach { who ->
                            val index = who * 3 + what
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        bits = bits.mapIndexed { i, on -> if (i == index) !on else on }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = bits[index],
                                    onCheckedChange = { checked ->
                                        bits = bits.mapIndexed { i, on -> if (i == index) checked else on }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Accent),
                                )
                                Text(text = stringResource(label), color = Ink, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("${stringResource(R.string.attr_permission)}  $octal", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text("${stringResource(R.string.attr_owner)}  ${current.owner}", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text("${stringResource(R.string.attr_group)}  ${current.group}", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.perm_special), color = Muted, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(R.string.perm_setuid to 4, R.string.perm_setgid to 2, R.string.perm_sticky to 1).forEach { (label, bit) ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { special = special xor bit },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = (special and bit) != 0,
                                onCheckedChange = { special = special xor bit },
                                colors = CheckboxDefaults.colors(checkedColor = Accent),
                            )
                            Text(text = stringResource(label), color = Ink, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                if (item.isDir) {
                    ApplyInsideRows(toFiles, { toFiles = it }, toFolders, { toFolders = it })
                }
                if (system) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.warn_system),
                        color = Danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { understood = !understood },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = understood, onCheckedChange = { understood = it })
                        Text(stringResource(R.string.warn_understand), color = Ink, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(octal, toFiles, toFolders) }, enabled = !system || understood) {
                Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Accent)
            }
        },
    )
}

/** Dua centang "Juga berlaku untuk semua file" dan "... semua folder", seperti MT Manager. */
@Composable
private fun ApplyInsideRows(
    toFiles: Boolean,
    onFiles: (Boolean) -> Unit,
    toFolders: Boolean,
    onFolders: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onFiles(!toFiles) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = toFiles, onCheckedChange = onFiles, colors = CheckboxDefaults.colors(checkedColor = Accent))
        Text(stringResource(R.string.perm_apply_files), color = Ink, fontSize = 14.sp)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onFolders(!toFolders) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = toFolders, onCheckedChange = onFolders, colors = CheckboxDefaults.colors(checkedColor = Accent))
        Text(stringResource(R.string.perm_apply_folders), color = Ink, fontSize = 14.sp)
    }
}

/**
 * Dialog ganti pemilik dan grup. Isinya nama (misalnya root, system, shell) atau angka UID/GID.
 * Hanya huruf, angka, dan garis bawah yang diterima, supaya aman dipakai di perintah shell.
 */
@Composable
private fun OwnerDialog(
    item: FileItem,
    current: Ownership,
    onApply: (String, String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var owner by remember { mutableStateOf(current.owner) }
    var group by remember { mutableStateOf(current.group) }
    var toFiles by remember { mutableStateOf(false) }
    var toFolders by remember { mutableStateOf(false) }
    val system = FileRepository.isSystemPath(item.path)
    var understood by remember { mutableStateOf(false) }
    val safe = Regex("[A-Za-z0-9_]+")
    val valid = safe.matches(owner.trim()) && safe.matches(group.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.attr_owner), color = Muted, fontSize = 13.sp)
                TextField(
                    value = owner,
                    onValueChange = { owner = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.attr_group), color = Muted, fontSize = 13.sp)
                TextField(
                    value = group,
                    onValueChange = { group = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Pilihan cepat untuk pemilik dan grup yang paling sering dipakai
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("root", "system", "shell", "media_rw").forEach { name ->
                        Text(
                            text = name,
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    owner = name
                                    group = name
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }
                }
                if (item.isDir) {
                    ApplyInsideRows(toFiles, { toFiles = it }, toFolders, { toFolders = it })
                }
                if (system) {
                    Text(
                        text = stringResource(R.string.warn_system),
                        color = Danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { understood = !understood },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = understood, onCheckedChange = { understood = it })
                        Text(stringResource(R.string.warn_understand), color = Ink, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(owner.trim(), group.trim(), toFiles, toFolders) },
                enabled = valid && (!system || understood),
            ) {
                Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Accent)
            }
        },
    )
}
