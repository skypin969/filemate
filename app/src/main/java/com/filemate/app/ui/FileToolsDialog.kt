package com.filemate.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon as ShortcutIcon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.filemate.app.MainActivity
import com.filemate.app.R
import com.filemate.app.data.FileItem
import com.filemate.app.data.FileRepository
import com.filemate.app.data.FileTools

/** Pilihan di dialog "Peralatan" (menu tekan lama), urutannya mengikuti MT Manager. */
enum class FileTool(val label: Int) {
    APK(R.string.btn_apk_tools),
    SPLIT(R.string.ftool_split),
    JOIN(R.string.ftool_join),
    ENCRYPT(R.string.ftool_encrypt),
    DECRYPT(R.string.ftool_decrypt),
    SWAP(R.string.ftool_swap),
    SYMLINK(R.string.ftool_symlink),
    SHORTCUT(R.string.ftool_shortcut),
    EXPORT(R.string.ftool_export),
}

/** True jika [tool] bisa dipakai untuk berkas-berkas [targets]. */
private fun toolEnabled(tool: FileTool, targets: List<FileItem>): Boolean {
    val one = targets.singleOrNull()
    val oneFile = one != null && !one.isDir
    return when (tool) {
        FileTool.APK -> one != null && one.isApk
        FileTool.SPLIT -> oneFile
        FileTool.JOIN -> oneFile && FileTools.isPart(one!!.name)
        FileTool.ENCRYPT -> oneFile && !one!!.name.endsWith(FileTools.ENCRYPTED_EXT)
        FileTool.DECRYPT -> oneFile && one!!.name.endsWith(FileTools.ENCRYPTED_EXT)
        FileTool.SWAP -> targets.size == 2
        FileTool.SYMLINK -> one != null
        FileTool.SHORTCUT -> one != null
        FileTool.EXPORT -> targets.isNotEmpty()
    }
}

/**
 * Dialog "Peralatan": daftar alat dengan lingkaran di kiri. Alat yang tidak cocok
 * untuk berkas yang dipilih tampil pudar dan tidak bisa diketuk.
 */
@Composable
fun FileToolsDialog(
    targets: List<FileItem>,
    onPick: (FileTool) -> Unit,
    onDismiss: () -> Unit,
) {
    // "Alat APK" hanya ditampilkan untuk berkas APK, supaya daftarnya sama seperti MT Manager untuk berkas lain
    val tools = FileTool.entries.filter { it != FileTool.APK || toolEnabled(it, targets) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)) {
                Text(
                    text = stringResource(R.string.menu_tools_title),
                    color = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
                )
                Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    tools.forEach { tool ->
                        val enabled = toolEnabled(tool, targets)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) { onPick(tool) }
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (enabled) MenuIcon else Disabled,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = stringResource(tool.label),
                                color = if (enabled) Ink else Disabled,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog "Ekstrak" seperti MT Manager: ekstrak ke folder ini atau ke jalur lain,
 * ikut folder panel sebelah, dan hapus arsip setelah selesai.
 * [onExtract] menerima folder tujuan dan pilihan hapus arsip.
 */
@Composable
fun ExtractDialog(
    path: String,
    otherPanelDir: String?,
    onExtract: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val name = FileRepository.nameOf(path)
    var intoHere by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf(name.substringBeforeLast('.', name) + "/") }
    var useOther by remember { mutableStateOf(false) }
    var deleteSource by remember { mutableStateOf(false) }
    val accent = RadioButtonDefaults.colors(selectedColor = Accent)
    val check = CheckboxDefaults.colors(checkedColor = Accent)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(stringResource(R.string.extract_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { intoHere = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = intoHere, onClick = { intoHere = true }, colors = accent)
                    Text(stringResource(R.string.extract_here), fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { intoHere = false },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = !intoHere, onClick = { intoHere = false }, colors = accent)
                    Text(stringResource(R.string.extract_path), fontSize = 14.sp)
                }
                if (!intoHere) {
                    TextField(
                        value = sub,
                        onValueChange = { sub = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (otherPanelDir != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { useOther = !useOther },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = useOther, onCheckedChange = { useOther = it }, colors = check)
                        Text(stringResource(R.string.extract_other_panel), fontSize = 14.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { deleteSource = !deleteSource },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = deleteSource, onCheckedChange = { deleteSource = it }, colors = check)
                    Text(stringResource(R.string.extract_delete_source), fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val base = if (useOther && otherPanelDir != null) otherPanelDir else FileRepository.parentOf(path)
                    val typed = sub.trim().trim('/')
                    // Jalur yang diawali "/" dipakai apa adanya, selain itu dianggap subfolder dari folder dasar
                    val target = when {
                        intoHere || typed.isEmpty() -> base
                        sub.trim().startsWith("/") -> "/$typed"
                        else -> FileRepository.join(base, typed)
                    }
                    onExtract(target, deleteSource)
                },
            ) {
                Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

/** Folder yang diminta dibuka lewat pintasan di layar utama HP. Dibaca layar utama, lalu dikosongkan. */
object PendingOpen {
    const val EXTRA_PATH = "com.filemate.app.OPEN_PATH"
    var path by mutableStateOf<String?>(null)
}

/**
 * Meminta peluncur HP menyematkan pintasan ke [item] di layar utama. Pintasan folder membuka folder itu,
 * pintasan berkas membuka folder tempat berkasnya. Mengembalikan false jika peluncur tidak mendukung.
 */
fun pinShortcut(context: Context, item: FileItem): Boolean = try {
    val manager = context.getSystemService(ShortcutManager::class.java)
    if (manager == null || !manager.isRequestPinShortcutSupported) {
        false
    } else {
        val folder = if (item.isDir) item.path else FileRepository.parentOf(item.path)
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(PendingOpen.EXTRA_PATH, folder)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val info = ShortcutInfo.Builder(context, "fm_" + Integer.toHexString(item.path.hashCode()))
            .setShortLabel(item.name.ifEmpty { "/" }.take(24))
            .setIcon(ShortcutIcon.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
        manager.requestPinShortcut(info, null)
    }
} catch (e: Exception) {
    false
}
