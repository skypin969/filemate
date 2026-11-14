package com.filemate.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filemate.app.R
import com.filemate.app.apk.ApkInfoReader
import java.io.File

/** Warna tombol kuning di kartu APK seperti di MT Manager. Tidak ikut warna tema. */
private val MtAmber = Color(0xFFFFB200)

/** Fungsi APK di MT Manager yang belum ada di Filemate. Tulisannya sama dengan MT; saat diketuk muncul pesan belum tersedia. */
private val MT_FUNCTIONS_NOT_YET = listOf(
    "Klon APK",
    "Pengoptimalan APK",
    "Dex Redivide",
    "Dex Anti-Kebingungan",
    "Dekripsi Dex String",
    "Kebingungan Sumber Daya",
    "Sumber Daya Anti-Kebingungan",
    "Minifikasi Sumber Daya",
    "Ganti Batch Xml",
    "Mode Terjemahan Xml",
    "Bunuh Verifikasi Tanda Tangan",
    "Pengoptimalan penggunaan kembali data",
    "Suntikkan penyedia dokumen",
    "Injeksi Log",
)

/** Satu baris di daftar FUNGSI. */
private class ApkFunction(val label: String, val onClick: () -> Unit)

/**
 * Kartu info APK seperti di MT Manager: ikon dan nama di atas, rincian di tengah,
 * lalu tombol FUNGSI, MELIHAT, dan INSTALL di bawah.
 */
@Composable
fun ApkDialog(
    info: ApkInfo,
    install: ApkInstallState?,
    onSign: () -> Unit,
    onUnpack: () -> Unit,
    onRepack: () -> Unit,
    onInstall: () -> Unit,
    onBrowse: () -> Unit,
    onHideInstall: () -> Unit,
    onCloseInstall: () -> Unit,
    onDetails: () -> Unit,
    onLaunch: () -> Unit,
    onDismiss: () -> Unit,
) {
    var functionsOpen by remember { mutableStateOf(false) }
    var manifestOpen by remember { mutableStateOf(false) }
    val amberButton = ButtonDefaults.textButtonColors(contentColor = MtAmber)
    val shown = !info.loading
    val noneText = stringResource(R.string.apk_none)
    val protectionNone = stringResource(R.string.apk_protection_none)
    val signatureText = if (shown) info.signature.ifEmpty { noneText } else ""
    val protectionText = if (shown) (info.protector ?: protectionNone) else ""

    Dialog(
        onDismissRequest = { if (!info.busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DialogBg,
            modifier = Modifier.fillMaxWidth(0.85f).widthIn(max = 420.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ApkIcon(path = info.path, size = 52.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.label.ifEmpty { File(info.path).name },
                            color = Ink,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(text = info.versionName, color = Muted, fontSize = 15.sp)
                    }
                }
                if (info.busy) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MtAmber)
                }
                Spacer(Modifier.height(14.dp))
                ApkInfoRow(stringResource(R.string.apk_row_package), if (shown) info.packageName else "")
                ApkInfoRow(
                    stringResource(R.string.apk_row_version_code),
                    if (shown) info.versionCode.toString() else "",
                )
                ApkInfoRow(stringResource(R.string.apk_row_size), if (shown) formatSize(info.size) else "")
                ApkInfoRow(stringResource(R.string.apk_row_signature), signatureText)
                ApkInfoRow(stringResource(R.string.apk_row_protection), protectionText)
                ApkInfoRow(
                    stringResource(R.string.apk_row_sdk_target),
                    if (shown) sdkText(info.targetSdk) else "",
                )
                ApkInfoRow(
                    stringResource(R.string.apk_row_sdk_min),
                    if (shown) sdkText(info.minSdk) else "",
                )
                if (info.installed) {
                    ApkInfoRow(stringResource(R.string.apk_row_install_path), info.installedPath, maxLines = 1)
                    ApkInfoRow(stringResource(R.string.apk_row_uid), info.installedUid.toString())
                }
                if (info.status.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(text = info.status, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { functionsOpen = true },
                        enabled = !info.busy,
                        colors = amberButton,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(stringResource(R.string.apk_btn_functions), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onBrowse,
                        enabled = !info.busy,
                        colors = amberButton,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(stringResource(R.string.apk_btn_view), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = onInstall,
                        enabled = !info.busy,
                        colors = amberButton,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(stringResource(R.string.apk_btn_install_card), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (functionsOpen) {
        ApkFunctionsDialog(
            onSign = {
                functionsOpen = false
                onSign()
            },
            onUnpack = {
                functionsOpen = false
                onUnpack()
            },
            onRepack = {
                functionsOpen = false
                onRepack()
            },
            onManifest = {
                functionsOpen = false
                manifestOpen = true
            },
            onDismiss = { functionsOpen = false },
        )
    }
    if (manifestOpen) {
        ApkManifestDialog(manifest = info.manifest, onDismiss = { manifestOpen = false })
    }
    if (install != null && install.visible) {
        ApkInstallDialog(
            install = install,
            onHide = onHideInstall,
            onClose = onCloseInstall,
            onDetails = onDetails,
            onLaunch = onLaunch,
        )
    }
}

/** Satu baris rincian: nama di kiri, nilai di kanan, seperti tabel di kartu MT Manager. */
@Composable
private fun ApkInfoRow(label: String, value: String, maxLines: Int = 2) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            color = Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            color = Muted,
            fontSize = 14.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Versi SDK seperti di MT Manager, misalnya "Android 14 (API 34)". */
@Composable
private fun sdkText(api: Int): String {
    if (api <= 1) return stringResource(R.string.apk_sdk_unknown)
    val name = ApkInfoReader.androidName(api)
    return if (name.isEmpty()) "API $api" else stringResource(R.string.apk_sdk_format, name, api)
}

/**
 * Daftar FUNGSI dengan urutan sama seperti di MT Manager. "Tanda APK" ada di atas karena itu fungsi yang sudah jalan.
 * Fungsi Filemate yang tidak ada di MT (Bongkar, Rakit ulang, Lihat AndroidManifest) ditaruh di bawah supaya tidak hilang.
 */
@Composable
private fun ApkFunctionsDialog(
    onSign: () -> Unit,
    onUnpack: () -> Unit,
    onRepack: () -> Unit,
    onManifest: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val unavailable = stringResource(R.string.apk_fn_unavailable)
    val menu = listOf(
        ApkFunction(stringResource(R.string.apk_fn_sign)) {
            onDismiss()
            onSign()
        },
    ) + MT_FUNCTIONS_NOT_YET.map { label ->
        ApkFunction(label) { Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show() }
    } + listOf(
        ApkFunction(stringResource(R.string.apk_btn_unpack)) {
            onDismiss()
            onUnpack()
        },
        ApkFunction(stringResource(R.string.apk_btn_repack)) {
            onDismiss()
            onRepack()
        },
        ApkFunction(stringResource(R.string.apk_fn_manifest)) {
            onDismiss()
            onManifest()
        },
    )
    // Tinggi popup mengikuti layar, seperti popup FUNGSI di MT Manager yang hampir setinggi layar
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DialogBg,
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = maxHeight),
        ) {
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                items(menu) { fn ->
                    ApkFunctionRow(label = fn.label, onClick = fn.onClick)
                }
            }
        }
    }
}

/** Satu baris FUNGSI. Tulisannya sama dengan MT Manager, tanpa keterangan tambahan. */
@Composable
private fun ApkFunctionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Ink, fontSize = 16.sp)
    }
}

/** Isi AndroidManifest sebagai teks, dibuka dari FUNGSI lalu Lihat AndroidManifest. */
@Composable
private fun ApkManifestDialog(manifest: String, onDismiss: () -> Unit) {
    val emptyText = stringResource(R.string.apk_manifest_empty)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text(stringResource(R.string.apk_manifest_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = manifest.ifEmpty { emptyText },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        },
    )
}

/** Dialog pemasangan: "Installing..." saat berjalan, lalu "Instalasi berhasil" atau "Instalasi gagal". */
@Composable
private fun ApkInstallDialog(
    install: ApkInstallState,
    onHide: () -> Unit,
    onClose: () -> Unit,
    onDetails: () -> Unit,
    onLaunch: () -> Unit,
) {
    val amberButton = ButtonDefaults.textButtonColors(contentColor = MtAmber)
    val titleRes = when (install.phase) {
        ApkPhase.RUNNING -> R.string.apk_install_running
        ApkPhase.DONE -> R.string.apk_install_done
        ApkPhase.FAILED -> R.string.apk_install_failed
    }
    Dialog(
        onDismissRequest = { if (install.phase == ApkPhase.RUNNING) onHide() else onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DialogBg,
            modifier = Modifier.fillMaxWidth(0.85f).widthIn(max = 420.dp),
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 22.dp, end = 24.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(titleRes),
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (install.phase == ApkPhase.RUNNING) {
                        TextButton(
                            onClick = onHide,
                            colors = amberButton,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(stringResource(R.string.apk_btn_hide), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(text = install.fileName, color = Ink, fontSize = 15.sp)
                if (install.phase == ApkPhase.FAILED && install.message.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = install.message, color = Muted, fontSize = 13.sp)
                }
                if (install.phase == ApkPhase.RUNNING) {
                    Spacer(Modifier.height(18.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MtAmber)
                    Spacer(Modifier.height(4.dp))
                } else {
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (install.phase == ApkPhase.DONE) {
                            TextButton(
                                onClick = onDetails,
                                colors = amberButton,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(stringResource(R.string.apk_btn_details), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = onClose,
                            colors = amberButton,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(stringResource(R.string.apk_btn_close_upper), fontWeight = FontWeight.SemiBold)
                        }
                        if (install.phase == ApkPhase.DONE) {
                            TextButton(
                                onClick = onLaunch,
                                colors = amberButton,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(stringResource(R.string.apk_btn_launch), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
