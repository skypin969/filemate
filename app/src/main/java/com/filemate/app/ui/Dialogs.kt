package com.filemate.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.io.File

/** Dialog berisi satu kolom isian, misalnya nama folder baru. */
@Composable
fun InputDialog(
    title: String,
    label: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // Kolom isian bergaris bawah tanpa kotak, seperti dialog NP Manager
            TextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(label) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}

/**
 * Dialog "Baru" dari tombol tambah, seperti NP Manager: satu kolom nama,
 * lalu pilih mau dibuat sebagai berkas atau folder. Nama yang sudah dipakai langsung ditandai.
 */
@Composable
fun NewItemDialog(
    parentDir: String,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val name = value.trim()
    val exists = name.isNotEmpty() && File(parentDir, name).exists()
    val usable = name.isNotEmpty() && !exists
    val supporting: (@Composable () -> Unit)? = if (exists) {
        { Text(stringResource(R.string.msg_name_exists), color = Danger) }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(stringResource(R.string.dialog_new_title), fontWeight = FontWeight.SemiBold) },
        text = {
            TextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                isError = exists,
                supportingText = supporting,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onCreateFile(name) }, enabled = usable) {
                    Text(stringResource(R.string.btn_new_file), fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { onCreateFolder(name) }, enabled = usable) {
                    Text(stringResource(R.string.btn_new_folder), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

/**
 * Dialog konfirmasi. Untuk hapus, tampil nama dan ukuran berkas serta pilihan "Pindah ke Recycle Bin"
 * seperti NP Manager. Untuk folder sistem, tombol baru aktif setelah user mencentang bahwa dia paham risikonya.
 * [single] diisi jika yang dihapus hanya satu berkas. [trashAvailable] false jika berkas tidak bisa masuk Recycle Bin.
 * [trashDefault] dan [showWarning] berasal dari pengaturan Recycle Bin.
 */
@Composable
fun ConfirmDialog(
    action: FileAction,
    system: Boolean,
    single: FileItem?,
    trashAvailable: Boolean,
    trashDefault: Boolean,
    showWarning: Boolean,
    onConfirm: (FileAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (action) {
        is FileAction.Delete -> stringResource(R.string.btn_delete)
        is FileAction.Paste -> stringResource(if (action.move) R.string.btn_move else R.string.btn_copy)
        is FileAction.Rename -> stringResource(R.string.confirm_rename_title)
        is FileAction.NewFolder -> stringResource(R.string.confirm_new_title)
        is FileAction.NewFile -> stringResource(R.string.confirm_new_title)
    }
    val body = when (action) {
        is FileAction.Delete -> if (single != null) {
            stringResource(R.string.confirm_delete_one, single.name)
        } else {
            stringResource(R.string.confirm_delete_many, action.paths.size)
        }
        // Seperti MT Manager: sebut nama berkas dan folder tujuannya
        is FileAction.Paste -> {
            val what = action.sources.singleOrNull()?.substringAfterLast('/')
                ?: stringResource(R.string.confirm_items, action.sources.size)
            stringResource(
                if (action.move) R.string.confirm_move_body else R.string.confirm_copy_body,
                what,
                action.destDir.trimEnd('/') + "/",
            )
        }
        is FileAction.Rename -> stringResource(R.string.confirm_rename_body)
        is FileAction.NewFolder -> stringResource(R.string.confirm_new_body)
        is FileAction.NewFile -> stringResource(R.string.confirm_new_body)
    }
    val isDelete = action is FileAction.Delete
    var understood by remember { mutableStateOf(false) }
    var toTrash by remember { mutableStateOf(trashAvailable && trashDefault) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(body, fontSize = 15.sp)
                if (isDelete && single != null && !single.isDir) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.attr_size) + "  " + formatSize(single.size),
                        color = Muted,
                        fontSize = 14.sp,
                    )
                }
                if (isDelete && trashAvailable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toTrash = !toTrash },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = toTrash,
                            onCheckedChange = { toTrash = it },
                            colors = CheckboxDefaults.colors(checkedColor = Accent),
                        )
                        Text(stringResource(R.string.delete_to_trash), fontSize = 14.sp)
                    }
                }
                // Peringatan merah saat berkas akan dihapus selamanya
                if (isDelete && showWarning && !(trashAvailable && toTrash)) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.delete_forever_warning), color = Danger, fontSize = 14.sp)
                }
                if (system) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.warn_system),
                        color = Danger,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = understood, onCheckedChange = { understood = it })
                        Text(stringResource(R.string.warn_understand), fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (action is FileAction.Delete) {
                        onConfirm(action.copy(toTrash = trashAvailable && toTrash))
                    } else {
                        onConfirm(action)
                    }
                },
                enabled = !system || understood,
            ) {
                Text(
                    text = stringResource(R.string.btn_ok),
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

/** Dialog alat APK: isi manifest, tiga tombol (bongkar, rakit ulang, tanda tangan). */
@Composable
fun ApkDialog(
    info: ApkInfo,
    onUnpack: () -> Unit,
    onRepack: () -> Unit,
    onSign: () -> Unit,
    onInstall: () -> Unit,
    onBrowse: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!info.busy) onDismiss() },
        title = { Text(File(info.path).name) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.apk_intro), color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                if (info.busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Accent)
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = stringResource(R.string.apk_manifest_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = SoftGray,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = info.manifest.ifEmpty { stringResource(R.string.apk_manifest_empty) },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp),
                    )
                }

                if (info.status.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = info.status,
                        color = Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onUnpack,
                        enabled = !info.busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.apk_btn_unpack), fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onRepack,
                        enabled = !info.busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.apk_btn_repack), fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onSign,
                        enabled = !info.busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.apk_btn_sign), fontSize = 13.sp)
                    }
                }
                OutlinedButton(
                    onClick = onInstall,
                    enabled = !info.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.apk_btn_install), fontSize = 13.sp)
                }
                // Membuka isi APK di panel seperti folder (tombol "Lihat" di MT Manager)
                OutlinedButton(
                    onClick = onBrowse,
                    enabled = !info.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.apk_btn_browse), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !info.busy) {
                Text(stringResource(R.string.btn_close))
            }
        },
    )
}
