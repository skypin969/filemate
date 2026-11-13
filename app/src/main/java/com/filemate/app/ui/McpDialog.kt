package com.filemate.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.filemate.app.R
import com.filemate.app.mcp.McpService
import com.filemate.app.mcp.McpTools

/**
 * Dialog APK MCP, susunannya mengikuti NP Manager: penjelasan, alamat lokal dan LAN,
 * sakelar, tombol pengoptimalan baterai, lalu tombol mulai, atur, dan tutup.
 */
@Composable
fun McpDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prefs = remember { AppPrefs.of(context) }
    val status by McpService.state.collectAsState()
    var auto by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_MCP_AUTO, false)) }
    var lan by remember { mutableStateOf(prefs.getBoolean(AppPrefs.KEY_MCP_LAN, false)) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    // Android 13 ke atas: izin notifikasi diminta dulu supaya notifikasi layanan terlihat.
    // Layanan tetap dimulai walau izin ditolak.
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        McpService.start(context)
    }

    fun startService() {
        val needAsk = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needAsk) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) else McpService.start(context)
    }

    val notStarted = stringResource(R.string.mcp_not_started)
    val copied = stringResource(R.string.mcp_copied)
    val localAddress = if (status.running) "http://127.0.0.1:${status.port}/mcp" else notStarted
    val lanIp = remember(status.running) { McpService.lanAddress() }
    val lanAddress = when {
        !status.running -> notStarted
        !status.lan -> stringResource(R.string.mcp_lan_off)
        lanIp == null -> stringResource(R.string.mcp_lan_no_network)
        else -> "http://$lanIp:${status.port}/mcp"
    }

    fun copy(address: String) {
        if (address.startsWith("http")) {
            clipboard.setText(AnnotatedString(address))
            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tool_apk_mcp),
                        color = Ink,
                        fontSize = 21.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Filled.History, stringResource(R.string.mcp_history), tint = Ink)
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.mcp_help), tint = Ink)
                    }
                }

                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.mcp_description),
                        color = Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(14.dp))

                    AddressBlock(stringResource(R.string.mcp_local_address), localAddress) { copy(localAddress) }
                    AddressBlock(stringResource(R.string.mcp_lan_address), lanAddress) { copy(lanAddress) }

                    val error = status.error
                    if (error != null) {
                        Text(
                            text = stringResource(R.string.mcp_start_failed, error),
                            color = Danger,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    SwitchRow(stringResource(R.string.mcp_auto_start), auto) {
                        auto = it
                        prefs.edit().putBoolean(AppPrefs.KEY_MCP_AUTO, it).apply()
                    }
                    SwitchRow(stringResource(R.string.mcp_allow_lan), lan) {
                        lan = it
                        prefs.edit().putBoolean(AppPrefs.KEY_MCP_LAN, it).apply()
                    }
                    if (status.running && lan != status.lan) {
                        Text(stringResource(R.string.mcp_restart_needed), color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.mcp_battery), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { if (status.running) McpService.stop(context) else startService() },
                    ) {
                        Text(
                            text = stringResource(if (status.running) R.string.mcp_stop else R.string.mcp_start),
                            color = Accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showSettings = true }) {
                        Text(stringResource(R.string.mcp_settings), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.mcp_close), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showSettings) {
        var portText by remember { mutableStateOf(prefs.getInt(AppPrefs.KEY_MCP_PORT, McpService.DEFAULT_PORT).toString()) }
        val port = portText.toIntOrNull()
        val valid = port != null && port in 1024..65535
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = Color.White,
            title = { Text(stringResource(R.string.mcp_settings), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(stringResource(R.string.mcp_port), color = Muted, fontSize = 13.sp)
                    TextField(
                        value = portText,
                        onValueChange = { portText = it.filter { ch -> ch.isDigit() }.take(5) },
                        singleLine = true,
                        isError = !valid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedIndicatorColor = Accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.mcp_port_hint), color = Muted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        prefs.edit().putInt(AppPrefs.KEY_MCP_PORT, port ?: McpService.DEFAULT_PORT).apply()
                        showSettings = false
                    },
                ) {
                    Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Accent)
                }
            },
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            containerColor = Color.White,
            title = { Text(stringResource(R.string.mcp_history), fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    if (status.history.isEmpty()) {
                        Text(stringResource(R.string.mcp_history_empty), color = Ink, fontSize = 14.sp)
                    }
                    status.history.forEach { line ->
                        Text(
                            text = line,
                            color = Ink,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) {
                    Text(stringResource(R.string.mcp_close), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    if (showHelp) {
        McpHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun AddressBlock(title: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 12.dp),
    ) {
        Text(text = title, color = MenuIcon, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text = value, color = Muted, fontSize = 14.sp)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Ink, fontSize = 15.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Accent),
        )
    }
}

/** Bantuan: langkah menyambungkan aplikasi AI, lalu daftar alat yang tersedia per kelompok. */
@Composable
private fun McpHelpDialog(onDismiss: () -> Unit) {
    val groups = listOf(
        McpTools.GROUP_OPEN to R.string.mcp_group_open,
        McpTools.GROUP_VIEW to R.string.mcp_group_view,
        McpTools.GROUP_DEX to R.string.mcp_group_dex,
        McpTools.GROUP_BUILD to R.string.mcp_group_build,
        McpTools.GROUP_FILES to R.string.mcp_group_files,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(stringResource(R.string.mcp_help), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.mcp_help_steps), color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.mcp_help_tools, McpTools.all.size),
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                groups.forEach { (group, title) ->
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(title), color = MenuIcon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    McpTools.all.filter { it.group == group }.forEach { tool ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tool.name,
                                color = Ink,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                            )
                            // Label kecil: alat ini hanya membaca, atau juga mengubah berkas
                            Text(
                                text = stringResource(if (tool.write) R.string.mcp_tag_write else R.string.mcp_tag_read),
                                color = if (tool.write) Danger else Muted,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(SoftGray, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mcp_close), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
