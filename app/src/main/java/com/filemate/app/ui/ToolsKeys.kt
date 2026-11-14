package com.filemate.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import com.filemate.app.apk.SigningKey
import com.filemate.app.apk.SigningKeys
import com.filemate.app.data.FileRepository
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Folder tempat berkas kunci tanda tangan diletakkan. */
private fun keysDir(): File = File(FileRepository.FILEMATE_DIR, "keys")

/** Keterangan singkat sebuah kunci: nama pemilik sertifikat dan sidik jari SHA-256-nya. */
private fun describeKey(key: SigningKey): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(key.certificate.encoded)
    val print = digest.joinToString(":") { "%02X".format(it) }
    return key.certificate.subjectX500Principal.name + "\nSHA-256: " + print
}

/**
 * Alat "Kunci tanda tangan": memilih kunci yang dipakai saat menandatangani APK.
 * Selain kunci bawaan Filemate, berkas kunci di folder Filemate/keys bisa dipakai:
 * pasangan .pk8 dan .x509.pem, atau keystore .p12, .pfx, .bks.
 */
@Composable
fun KeyManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs.of(context) }
    var active by remember { mutableStateOf(SigningKeys.customPath) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var info by remember { mutableStateOf("") }
    // Berkas keystore yang sedang menunggu kata sandi
    var askPassword by remember { mutableStateOf<File?>(null) }
    val failed = stringResource(R.string.keys_failed)

    LaunchedEffect(Unit) {
        files = withContext(Dispatchers.IO) {
            keysDir().mkdirs()
            keysDir().listFiles().orEmpty()
                .filter { it.isFile && it.extension.lowercase() in SigningKeys.KEY_EXTENSIONS }
                .sortedBy { it.name.lowercase() }
        }
    }
    // Keterangan kunci yang sedang dipakai
    LaunchedEffect(active) {
        info = withContext(Dispatchers.IO) {
            try {
                describeKey(SigningKeys.load())
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun use(path: String, password: String) {
        SigningKeys.customPath = path
        SigningKeys.customPassword = password
        prefs.edit().putString(AppPrefs.KEY_SIGN_KEY, path).putString(AppPrefs.KEY_SIGN_PASS, password).apply()
        active = path
    }

    /** Mencoba membaca [file] dulu. Kunci baru dipakai jika berhasil dibaca. */
    fun tryUse(file: File, password: String) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { SigningKeys.loadFile(file, password) != null }
            if (ok) use(file.path, password) else Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
        }
    }

    ToolScaffold(title = stringResource(R.string.tool_keys), onBack = onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            KeyRow(
                title = stringResource(R.string.keys_builtin),
                subtitle = stringResource(R.string.keys_builtin_desc),
                selected = active.isEmpty(),
                onClick = { use("", "") },
            )
            files.forEach { file ->
                KeyRow(
                    title = file.name,
                    subtitle = file.parent.orEmpty(),
                    selected = active == file.path,
                    onClick = {
                        // Berkas pk8 tidak berkata sandi. Keystore ditanya kata sandinya dulu.
                        if (file.extension.equals("pk8", ignoreCase = true)) tryUse(file, "") else askPassword = file
                    },
                )
            }
            if (info.isNotEmpty()) {
                Text(
                    text = info,
                    color = Muted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
            Text(
                text = stringResource(R.string.keys_help, keysDir().path),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }

    askPassword?.let { file ->
        PasswordDialog(
            title = file.name,
            onConfirm = { typed ->
                askPassword = null
                tryUse(file, typed)
            },
            onDismiss = { askPassword = null },
        )
    }
}

@Composable
private fun KeyRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.VpnKey, contentDescription = null, tint = MenuIcon, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Accent else Muted,
        )
    }
}

/** Daftar kata sandi umum yang tersimpan, satu per baris di pengaturan aplikasi. */
object CommonPasswords {
    fun load(context: Context): List<String> =
        AppPrefs.of(context).getString(AppPrefs.KEY_PASSWORDS, "").orEmpty().lines().filter { it.isNotEmpty() }

    fun save(context: Context, list: List<String>) {
        AppPrefs.of(context).edit().putString(AppPrefs.KEY_PASSWORDS, list.distinct().joinToString("\n")).apply()
    }
}

/**
 * Alat "Kata sandi umum": daftar kata sandi yang sering dipakai, supaya bisa dipilih dengan satu
 * ketukan saat mengenkripsi, mendekripsi, atau membuka keystore. Disimpan apa adanya di HP ini.
 */
@Composable
fun PasswordsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var list by remember { mutableStateOf(CommonPasswords.load(context)) }
    var adding by remember { mutableStateOf(false) }

    fun update(next: List<String>) {
        list = next.distinct()
        CommonPasswords.save(context, list)
    }

    ToolScaffold(title = stringResource(R.string.tool_passwords), onBack = onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            if (list.isEmpty()) {
                Text(
                    text = stringResource(R.string.passwords_empty),
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(18.dp),
                )
            }
            list.forEach { password ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = password,
                        color = Ink,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { update(list - password) }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.btn_delete), tint = Muted)
                    }
                }
            }
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                PillButton(stringResource(R.string.passwords_add)) { adding = true }
            }
            Text(
                text = stringResource(R.string.passwords_note),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            )
        }
    }

    if (adding) {
        InputDialog(
            title = stringResource(R.string.passwords_add),
            label = stringResource(R.string.ftool_password_hint),
            initial = "",
            confirmText = stringResource(R.string.btn_ok),
            onConfirm = { typed ->
                adding = false
                update(list + typed)
            },
            onDismiss = { adding = false },
        )
    }
}

/** Dialog isian kata sandi. Kata sandi umum yang tersimpan tampil di bawahnya dan bisa diketuk untuk dipakai. */
@Composable
fun PasswordDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val saved = remember { CommonPasswords.load(context) }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text(stringResource(R.string.ftool_password_hint)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (saved.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.tool_passwords),
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Column(modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())) {
                        saved.forEach { password ->
                            Text(
                                text = password,
                                color = Ink,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (password == value) SelectTint else Color.Transparent)
                                    .clickable { value = password }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotEmpty()) {
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
