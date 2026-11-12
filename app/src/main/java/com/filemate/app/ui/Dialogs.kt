package com.filemate.app.ui

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
 * Dialog konfirmasi. Untuk folder sistem, tombol baru aktif setelah user
 * mencentang bahwa dia paham risikonya.
 */
@Composable
fun ConfirmDialog(
    action: FileAction,
    system: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (action) {
        is FileAction.Delete -> stringResource(R.string.confirm_delete_title)
        is FileAction.Paste -> stringResource(R.string.confirm_paste_title)
        is FileAction.Rename -> stringResource(R.string.confirm_rename_title)
        is FileAction.NewFolder -> stringResource(R.string.confirm_new_title)
    }
    val body = when (action) {
        is FileAction.Delete -> stringResource(R.string.confirm_delete_body, action.paths.size)
        is FileAction.Paste -> stringResource(R.string.confirm_paste_body, action.sources.size)
        is FileAction.Rename -> stringResource(R.string.confirm_rename_body)
        is FileAction.NewFolder -> stringResource(R.string.confirm_new_body)
    }
    val danger = action is FileAction.Delete
    val confirmText = stringResource(if (danger) R.string.btn_delete else R.string.btn_continue)
    var understood by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(body, fontSize = 15.sp)
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
                onClick = onConfirm,
                enabled = !system || understood,
            ) {
                Text(
                    text = confirmText,
                    color = if (danger) Danger else Accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !info.busy) {
                Text(stringResource(R.string.btn_close))
            }
        },
    )
}
