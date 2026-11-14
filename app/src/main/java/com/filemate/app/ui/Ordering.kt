package com.filemate.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filemate.app.R

/** Urutan pilihan di menu tekan lama. Bisa diubah di Pengaturan ("Urutkan menu berkas"). */
object MenuOrder {
    /** Kunci tiap pilihan dan namanya, dalam urutan bawaan. */
    val ITEMS: List<Pair<String, Int>> = listOf(
        "copy" to R.string.btn_copy,
        "move" to R.string.btn_move,
        "rename" to R.string.btn_rename,
        "delete" to R.string.btn_delete,
        "compress" to R.string.menu_compress,
        "attributes" to R.string.menu_attributes,
        "send" to R.string.menu_send_device,
        "share" to R.string.menu_share,
        "openwith" to R.string.menu_open_with,
        "hex" to R.string.menu_edit_hex,
        "tools" to R.string.menu_tools,
        "edittext" to R.string.menu_edit_text,
        "bookmark" to R.string.menu_bookmark,
        "extra" to R.string.menu_order_extra,
        "sign" to R.string.menu_sign,
    )

    /** Urutan kunci yang tersimpan. Kunci yang belum ada di simpanan (pilihan baru) ditaruh di belakang. */
    fun load(context: Context): List<String> =
        merge(AppPrefs.of(context).getString(AppPrefs.KEY_MENU_ORDER, "").orEmpty(), ITEMS.map { it.first })

    fun save(context: Context, order: List<String>) {
        AppPrefs.of(context).edit().putString(AppPrefs.KEY_MENU_ORDER, order.joinToString(",")).apply()
    }

    /** Menggabungkan urutan tersimpan dengan daftar kunci yang berlaku sekarang. */
    fun merge(saved: String, known: List<String>): List<String> {
        val kept = saved.split(',').map { it.trim() }.filter { it in known }.distinct()
        return kept + known.filterNot { it in kept }
    }
}

/**
 * Dialog untuk mengatur urutan: setiap baris punya panah naik dan turun.
 * [items] berisi kunci dan nama yang tampil. [onSave] menerima kunci dalam urutan baru.
 */
@Composable
fun ReorderDialog(
    title: String,
    items: List<Pair<String, String>>,
    onSave: (List<String>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var order by remember { mutableStateOf(items) }

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
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                order.forEachIndexed { index, (_, label) ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
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
            TextButton(onClick = { onSave(order.map { it.first }) }) {
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
