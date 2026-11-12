package com.filemate.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.filemate.app.R

/** Layar izin akses semua berkas. Layar ini tampil sampai izin diberikan. */
@Composable
fun PermissionScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Setiap kali kembali ke aplikasi, cek apakah izin sudah diberikan
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && Environment.isExternalStorageManager()) {
                onGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Biru hanya di belakang bilah status, supaya ikon putihnya tetap terlihat
            .background(NpBlue)
            .statusBarsPadding()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SelectTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = NpBlue,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.perm_title),
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 31.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.perm_body),
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SoftGray)
                .padding(16.dp),
        ) {
            Text("1. " + stringResource(R.string.perm_step_1), color = Ink, fontSize = 15.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("2. " + stringResource(R.string.perm_step_2), color = Ink, fontSize = 15.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("3. " + stringResource(R.string.perm_step_3), color = Ink, fontSize = 15.sp, lineHeight = 24.sp)
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.perm_note),
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { openAllFilesSettings(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text(
                text = stringResource(R.string.perm_open_settings),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = {
                if (Environment.isExternalStorageManager()) {
                    onGranted()
                } else {
                    Toast.makeText(context, R.string.perm_not_yet, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = stringResource(R.string.perm_check_again),
                color = Accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Membuka halaman "Akses semua berkas" untuk aplikasi ini. */
private fun openAllFilesSettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")
    val intents = listOf(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
    )
    for (intent in intents) {
        val opened = try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
        if (opened) return
    }
}
