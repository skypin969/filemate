package com.filemate.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

// Warna mengikuti NP Manager (diambil dari tangkapan layar)
val NpBlue = Color(0xFF409EFF)        // bilah atas, ikon folder
val NpBlueDark = Color(0xFF2B69AA)    // garis penanda panel aktif
val Accent = Color(0xFF3076BF)        // tombol teks, judul bagian, sakelar
val SoftGray = Color(0xFFF5F5F5)      // latar kotak lembut
val SelectTint = Color(0xFFDCEBFF)    // baris yang dipilih
val IconBox = Color(0xFFE8F3FF)       // kotak di belakang ikon menu samping
val PageGray = Color(0xFFFAFAFA)      // latar halaman pengaturan
val Ink = Color(0xFF111111)           // teks utama
val Muted = Color(0xFF646464)         // teks kecil, ikon bilah bawah
val Disabled = Color(0xFFC8C8C8)      // ikon yang tidak aktif
val LineColor = Color(0xFFE3E3E3)     // garis pemisah
val Danger = Color(0xFFC62828)        // hapus
val FileTint = Color(0xFF546E7A)      // ikon berkas biasa
val MenuIcon = Color(0xFF424242)      // ikon di menu

val HeaderBrush: Brush = SolidColor(NpBlue)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = SelectTint,
    onPrimaryContainer = Ink,
    secondary = NpBlue,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceContainerHigh = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    error = Danger,
)

@Composable
fun FilemateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
