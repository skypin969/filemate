package com.filemate.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.filemate.app.R

/**
 * Warna tema. Bawaannya hitam putih. Dari Pengaturan tema, user bisa mengganti warna utamanya
 * (bilah atas, ikon folder, tombol). Latar tetap putih, tidak ada mode gelap.
 */
object ThemeColors {
    const val DEFAULT = 0xFF111111.toInt()

    /** Pilihan warna di dialog Pengaturan tema. Yang pertama adalah hitam bawaan Filemate. */
    val OPTIONS: List<Int> = listOf(
        0xFF111111, 0xFF409EFF, 0xFF5DBB7A, 0xFFE5A23D, 0xFFF56C6C, 0xFF8A9583, 0xFF606266, 0xFF303133,
        0xFFFF5E9E, 0xFFFF0000, 0xFF9B1FFF, 0xFF002FA7, 0xFF81D8CF, 0xFF003152, 0xFF6667AB, 0xFF008982,
        0xFFC30D2E, 0xFFAB5621, 0xFF470024, 0xFF4C0009, 0xFFE95A20, 0xFFFBD26A,
    ).map { it.toInt() }

    /** Warna utama yang sedang dipakai. Layar ikut berubah begitu nilainya diganti. */
    var primary by mutableStateOf(DEFAULT)
}

/** Menggelapkan warna: 1 berarti tetap, 0 berarti hitam. */
private fun darken(color: Color, factor: Float): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor)

val Primary: Color get() = Color(ThemeColors.primary)      // bilah atas, layar pembuka

val PrimaryDark: Color get() = darken(Primary, 0.7f)       // versi lebih gelap dari warna utama

// Tombol teks, judul bagian, sakelar. Dibuat cukup gelap supaya tetap terbaca di latar putih,
// juga saat warna tema yang dipilih terang (kuning, toska muda).
val Accent: Color
    get() {
        if (ThemeColors.primary == ThemeColors.DEFAULT) return Primary
        val base = darken(Primary, 0.75f)
        return if (base.luminance() > 0.35f) darken(base, 0.6f) else base
    }

// Ikon folder: abu-abu gelap pada tema hitam putih, selain itu mengikuti warna tema
val FolderColor: Color
    get() = if (ThemeColors.primary == ThemeColors.DEFAULT) Color(0xFF2F2F2F) else Primary

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

val HeaderBrush: Brush get() = SolidColor(Primary)

private fun appColors() = lightColorScheme(
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
    // Skema warna dibuat ulang setiap warna tema berganti
    val colors = remember(ThemeColors.primary) { appColors() }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
