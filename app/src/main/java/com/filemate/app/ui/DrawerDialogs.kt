package com.filemate.app.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R

/** Dialog yang dibuka dari menu samping. Disimpan di MainScreen supaya menu samping bisa ditutup dulu. */
enum class DrawerDialog { REMOTE, PLUGINS, TOOL_GROUP, ADD_LOCAL, FOLLOW_INFO }

/**
 * Alat di menu samping beserta kuncinya, dalam urutan bawaan. Kunci inilah yang disimpan untuk
 * urutan dan alat yang disembunyikan. Sebelas pertama mengikuti MT Manager, sisanya alat khusus Filemate.
 */
val DRAWER_TOOL_LABELS: List<Pair<String, Int>> = listOf(
    "trash" to R.string.drawer_trash,
    "plugins" to R.string.drawer_plugins,
    "remote" to R.string.drawer_remote,
    "color" to R.string.drawer_color,
    "keys" to R.string.drawer_keys,
    "passwords" to R.string.drawer_passwords,
    "extract_apk" to R.string.drawer_extract_apk,
    "text" to R.string.drawer_text,
    "terminal" to R.string.drawer_terminal,
    "activity" to R.string.drawer_activity,
    "smali" to R.string.drawer_smali,
    "apps" to R.string.tool_apps,
    "toolbox" to R.string.tool_toolbox,
    "mcp" to R.string.tool_apk_mcp,
    "ftp" to R.string.tool_ftp,
    "http" to R.string.tool_http,
    "lan" to R.string.tool_lan,
    "java2smali" to R.string.tool_java2smali,
    "network" to R.string.tool_network,
    "layout" to R.string.tool_layout_viewer,
    "ruler" to R.string.tool_ruler,
    "decibel" to R.string.tool_decibel,
    "reflection" to R.string.tool_reflection,
    "classes" to R.string.tool_class_analysis,
    "timestamp" to R.string.tool_timestamp,
    "prefs" to R.string.drawer_settings,
)

/** Urutan alat yang berlaku: urutan yang tersimpan, dengan alat baru ditaruh di belakang. */
fun drawerToolOrder(saved: String): List<String> =
    MenuOrder.merge(saved, DRAWER_TOOL_LABELS.map { it.first })

/** Nama versi dari manifest, ditampilkan di kepala menu samping. */
@Suppress("DEPRECATION")
fun appVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
} catch (e: Exception) {
    ""
}

/**
 * Kelola grup alat: centang untuk menampilkan atau menyembunyikan, panah untuk mengatur urutan.
 * [items] berisi kunci dan nama dalam urutan sekarang. [onSave] menerima urutan baru dan alat yang disembunyikan.
 */
@Composable
fun ToolGroupDialog(
    items: List<Pair<String, String>>,
    hidden: Set<String>,
    onSave: (List<String>, Set<String>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var order by remember { mutableStateOf(items) }
    var hiddenNow by remember { mutableStateOf(hidden) }

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target !in order.indices) return
        val changed = order.toMutableList()
        val moved = changed.removeAt(index)
        changed.add(target, moved)
        order = changed
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.menu_manage_tools), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.tools_group_hint),
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                order.forEachIndexed { index, (key, label) ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = key !in hiddenNow,
                            onCheckedChange = { shown ->
                                hiddenNow = if (shown) hiddenNow - key else hiddenNow + key
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Accent),
                        )
                        Text(
                            text = label,
                            color = Ink,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { move(index, -1) }, enabled = index > 0) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                stringResource(R.string.order_up),
                                tint = if (index > 0) MenuIcon else Disabled,
                            )
                        }
                        IconButton(onClick = { move(index, 1) }, enabled = index < order.lastIndex) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                stringResource(R.string.order_down),
                                tint = if (index < order.lastIndex) MenuIcon else Disabled,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(order.map { it.first }, hiddenNow) }) {
                Text(stringResource(R.string.btn_ok), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.order_reset), color = Accent)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel), color = Accent)
                }
            }
        },
    )
}

/** Manajemen Jarak Jauh: pintu masuk ke Layanan FTP, Layanan HTTP, dan Transfer LAN. */
@Composable
fun RemoteHubDialog(onPick: (ToolKind) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.drawer_remote), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                HubRow(R.string.tool_ftp, Icons.Filled.Cloud) { onPick(ToolKind.FTP) }
                HubRow(R.string.tool_http, Icons.Filled.Language) { onPick(ToolKind.HTTP) }
                HubRow(R.string.tool_lan, Icons.Filled.Wifi) { onPick(ToolKind.LAN) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close), color = Accent)
            }
        },
    )
}

/** Satu baris di Manajemen Jarak Jauh: ikon, lalu nama layanannya. */
@Composable
private fun HubRow(label: Int, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MenuIcon, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = stringResource(label), color = Ink, fontSize = 15.sp)
    }
}

/** Manajer Plugin. Filemate belum bisa memuat plugin MT Manager, jadi daftarnya kosong. */
@Composable
fun PluginManagerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.drawer_plugins), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(stringResource(R.string.plugins_empty), color = Ink, fontSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.plugins_note), color = Muted, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close), color = Accent)
            }
        },
    )
}

/** Peringatan saat sakelar tema ditekan padahal "Tema mengikuti sistem" masih aktif. */
@Composable
fun FollowSystemDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        text = { Text(stringResource(R.string.theme_follow_info), color = Ink, fontSize = 15.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.btn_oke), color = Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel_upper), color = Accent)
            }
        },
    )
}
