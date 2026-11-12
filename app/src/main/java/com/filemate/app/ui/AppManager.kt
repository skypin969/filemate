package com.filemate.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.filemate.app.R
import com.filemate.app.data.FileRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Satu aplikasi terpasang di daftar. */
private class AppRow(
    val label: String,
    val packageName: String,
    val version: String,
    val apkPath: String,
    val size: Long,
    val splitSize: Long,
    val system: Boolean,
    val enabled: Boolean,
    val updated: Long,
)

private enum class AppFilter { ALL, ENABLED, DISABLED }

private fun loadApps(context: Context): List<AppRow> {
    val pm = context.packageManager
    return pm.getInstalledPackages(0).mapNotNull { info ->
        val app = info.applicationInfo ?: return@mapNotNull null
        val splits = app.splitSourceDirs.orEmpty().sumOf { File(it).length() }
        AppRow(
            label = app.loadLabel(pm).toString(),
            packageName = info.packageName,
            version = info.versionName ?: "",
            apkPath = app.sourceDir.orEmpty(),
            size = File(app.sourceDir.orEmpty()).length(),
            splitSize = splits,
            system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            enabled = app.enabled,
            updated = info.lastUpdateTime,
        )
    }.sortedByDescending { it.updated }
}

private val appIconCache = ConcurrentHashMap<String, ImageBitmap>()

private fun loadAppIcon(context: Context, packageName: String): ImageBitmap? = try {
    val image = context.packageManager.getApplicationIcon(packageName).toBitmap(120, 120).asImageBitmap()
    appIconCache[packageName] = image
    image
} catch (e: Exception) {
    null
}

/** Menyalin APK aplikasi ke folder Filemate/apk. Mengembalikan lokasi hasilnya, atau null jika gagal. */
private fun extractApk(app: AppRow): String? = try {
    val dir = File(FileRepository.FILEMATE_DIR, "apk")
    dir.mkdirs()
    val safe = app.label.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { app.packageName }
    val dest = File(dir, "${safe}_${app.version.ifBlank { "0" }}.apk")
    File(app.apkPath).copyTo(dest, overwrite = true)
    dest.path
} catch (e: Exception) {
    null
}

/**
 * Manajemen aplikasi seperti NP Manager: dua tab (pengguna dan sistem), saringan berbentuk pil,
 * dan kartu per aplikasi dengan tombol untuk mengambil APK-nya.
 */
@Composable
fun AppManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppRow>?>(null) }
    var systemTab by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(AppFilter.ALL) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    BackHandler {
        if (searching) {
            searching = false
            query = ""
        } else {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }

    val all = apps.orEmpty()
    val userCount = all.count { !it.system }
    val systemCount = all.size - userCount
    val shown = all.filter { app ->
        app.system == systemTab &&
            when (filter) {
                AppFilter.ALL -> true
                AppFilter.ENABLED -> app.enabled
                AppFilter.DISABLED -> !app.enabled
            } &&
            (query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize().background(PageGray)) {
        // Bilah atas biru
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBrush)
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White,
                )
            }
            if (searching) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                IconButton(
                    onClick = {
                        searching = false
                        query = ""
                    },
                ) {
                    Icon(Icons.Filled.Close, stringResource(R.string.cd_close), tint = Color.White)
                }
            } else {
                Text(
                    text = stringResource(R.string.tool_apps),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                IconButton(onClick = { searching = true }) {
                    Icon(Icons.Filled.Search, stringResource(R.string.menu_search), tint = Color.White)
                }
            }
        }

        // Dua tab dengan garis bawah pada tab yang aktif
        Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
            AppTab(
                text = stringResource(R.string.apps_tab_user, userCount),
                selected = !systemTab,
                onClick = { systemTab = false },
                modifier = Modifier.weight(1f),
            )
            AppTab(
                text = stringResource(R.string.apps_tab_system, systemCount),
                selected = systemTab,
                onClick = { systemTab = true },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(
                if (systemTab) R.string.apps_sorted_system else R.string.apps_sorted_user,
            ),
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
        ) {
            FilterPill(stringResource(R.string.apps_filter_all), filter == AppFilter.ALL) { filter = AppFilter.ALL }
            FilterPill(stringResource(R.string.apps_filter_enabled), filter == AppFilter.ENABLED) {
                filter = AppFilter.ENABLED
            }
            FilterPill(stringResource(R.string.apps_filter_disabled), filter == AppFilter.DISABLED) {
                filter = AppFilter.DISABLED
            }
        }
        Spacer(Modifier.height(6.dp))

        if (apps == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            items(shown, key = { it.packageName }) { app ->
                AppCard(
                    app = app,
                    onOpen = {
                        // Membuka halaman info aplikasi di pengaturan Android
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:${app.packageName}")),
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExtract = {
                        scope.launch {
                            val path = withContext(Dispatchers.IO) { extractApk(app) }
                            val message = if (path != null) {
                                context.getString(R.string.apps_extracted, path)
                            } else {
                                context.getString(R.string.apps_extract_failed)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            color = if (selected) Accent else MenuIcon,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) Accent else Color.Transparent),
        )
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.White else MenuIcon,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) Accent else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun AppCard(app: AppRow, onOpen: () -> Unit, onExtract: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState(initialValue = appIconCache[app.packageName], app.packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { loadAppIcon(context, app.packageName) }
        }
    }
    val detail = buildString {
        append(app.version)
        append("   ")
        append(formatSize(app.size))
        if (app.splitSize > 0) {
            append("  SPLIT+")
            append(formatSize(app.splitSize))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val loaded = icon
        if (loaded != null) {
            Image(bitmap = loaded, contentDescription = null, modifier = Modifier.size(46.dp))
        } else {
            Icon(
                imageVector = Icons.Filled.Android,
                contentDescription = null,
                tint = MenuIcon,
                modifier = Modifier.size(46.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = MenuIcon,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = detail, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = app.packageName,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IconBox)
                .clickable(onClick = onExtract),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = stringResource(R.string.apps_extract),
                tint = Accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
