package com.filemate.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R
import com.filemate.app.mcp.McpService
import com.filemate.app.net.NetServers
import com.filemate.app.net.ServerInfo

/**
 * Dialog untuk Layanan FTP, Layanan HTTP, dan Transfer LAN. Ketiganya serupa:
 * penjelasan, alamat untuk dibuka dari perangkat lain (ketuk untuk menyalin), dan tombol mulai atau hentikan.
 */
@Composable
fun NetServiceDialog(kind: ToolKind, onDismiss: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val clipboard = LocalClipboardManager.current
    val status by NetServers.state.collectAsState()
    val copied = stringResource(R.string.tool_copied)

    val title = stringResource(
        when (kind) {
            ToolKind.FTP -> R.string.tool_ftp
            ToolKind.HTTP -> R.string.tool_http
            else -> R.string.tool_lan
        },
    )
    val description = stringResource(
        when (kind) {
            ToolKind.FTP -> R.string.net_ftp_desc
            ToolKind.HTTP -> R.string.net_http_desc
            else -> R.string.net_lan_desc
        },
    )
    val info: ServerInfo? = when (kind) {
        ToolKind.FTP -> status.ftp
        ToolKind.HTTP -> status.http
        else -> status.lan
    }
    val ip = remember(info) { McpService.lanAddress() }
    val address = when {
        info == null -> null
        ip == null -> null
        kind == ToolKind.FTP -> "ftp://$ip:${info.port}"
        else -> "http://$ip:${info.port}/${info.token}/"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(description, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(14.dp))
                when {
                    info == null -> Text(stringResource(R.string.mcp_not_started), color = Ink, fontSize = 15.sp)
                    address == null -> Text(stringResource(R.string.mcp_lan_no_network), color = Danger, fontSize = 14.sp)
                    else -> {
                        Text(stringResource(R.string.net_address), color = MenuIcon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = address,
                            color = Ink,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboard.setText(AnnotatedString(address))
                                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 6.dp),
                        )
                        if (kind == ToolKind.FTP) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.net_ftp_login, info.user, info.password),
                                color = Ink,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.net_same_wifi), color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
                val error = status.error
                if (error != null && info == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.mcp_start_failed, error), color = Danger, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mcp_close), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    when (kind) {
                        ToolKind.FTP -> if (info == null) NetServers.startFtp(context) else NetServers.stopFtp(context)
                        ToolKind.HTTP -> if (info == null) NetServers.startHttp(context, title) else NetServers.stopHttp(context)
                        else -> if (info == null) NetServers.startLan(context, title) else NetServers.stopLan(context)
                    }
                },
            ) {
                Text(
                    text = stringResource(if (info == null) R.string.mcp_start else R.string.mcp_stop),
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}
