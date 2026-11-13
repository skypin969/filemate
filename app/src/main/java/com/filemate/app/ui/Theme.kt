package com.filemate.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.filemate.app.R

// Tema hitam putih: satu tampilan tetap (bukan mode gelap/terang). Bilah atas hitam, latar putih.
// Satu-satunya warna lain adalah merah untuk tindakan hapus, supaya tidak tertukar.
val Primary = Color(0xFF111111)       // bilah atas, layar pembuka
val PrimaryDark = Color(0xFF111111)   // garis penanda panel aktif
val Accent = Color(0xFF111111)        // tombol teks, judul bagian, sakelar
val FolderColor = Color(0xFF2F2F2F)   // ikon folder
val SoftGray = Color(0xFFF5F5F5)      // latar kotak lembut
val SelectTint = Color(0xFFE2E2E2)    // baris yang dipilih
val IconBox = Color(0xFFEDEDED)       // kotak di belakang ikon menu samping
val PageGray = Color(0xFFFAFAFA)      // latar halaman pengaturan
val Ink = Color(0xFF111111)           // teks utama
val Muted = Color(0xFF646464)         // teks kecil, ikon bilah bawah
val Disabled = Color(0xFFC8C8C8)      // ikon yang tidak aktif
val LineColor = Color(0xFFE3E3E3)     // garis pemisah
val Danger = Color(0xFFC62828)        // hapus
val FileTint = Color(0xFF757575)      // ikon berkas biasa
val MenuIcon = Color(0xFF424242)      // ikon di menu

val HeaderBrush: Brush = SolidColor(Primary)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = SelectTint,
    onPrimaryContainer = Ink,
    secondary = Primary,
    secondaryContainer = SelectTint,
    onSecondaryContainer = Ink,
    surfaceVariant = SoftGray,
    onSurfaceVariant = Muted,
    outline = Muted,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceContainerHigh = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    error = Danger,
)

// Font aplikasi: Plus Jakarta Sans, dibawa di dalam aplikasi supaya tidak bergantung pada font sistem HP.
val AppFont = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

// Semua gaya teks bawaan memakai font aplikasi. Teks berhuruf mesin tik (kode, terminal) tetap seperti semula.
private val Base = Typography()
private val AppTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = AppFont),
    displayMedium = Base.displayMedium.copy(fontFamily = AppFont),
    displaySmall = Base.displaySmall.copy(fontFamily = AppFont),
    headlineLarge = Base.headlineLarge.copy(fontFamily = AppFont),
    headlineMedium = Base.headlineMedium.copy(fontFamily = AppFont),
    headlineSmall = Base.headlineSmall.copy(fontFamily = AppFont),
    titleLarge = Base.titleLarge.copy(fontFamily = AppFont),
    titleMedium = Base.titleMedium.copy(fontFamily = AppFont),
    titleSmall = Base.titleSmall.copy(fontFamily = AppFont),
    bodyLarge = Base.bodyLarge.copy(fontFamily = AppFont),
    bodyMedium = Base.bodyMedium.copy(fontFamily = AppFont),
    bodySmall = Base.bodySmall.copy(fontFamily = AppFont),
    labelLarge = Base.labelLarge.copy(fontFamily = AppFont),
    labelMedium = Base.labelMedium.copy(fontFamily = AppFont),
    labelSmall = Base.labelSmall.copy(fontFamily = AppFont),
)

@Composable
fun FilemateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = AppTypography, content = content)
}
