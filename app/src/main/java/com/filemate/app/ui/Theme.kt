package com.filemate.app.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Mode tampilan seperti MT Manager: terang, gelap, atau ikut tema HP ("Tema mengikuti sistem").
 * [dark] adalah hasil akhirnya dan dipakai semua warna di bawah. Diatur oleh [FilemateTheme].
 */
object ThemeMode {
    /** Mode gelap yang dipilih user lewat ikon matahari/bulan di menu samping. */
    var manualDark by mutableStateOf(false)

    /** "Tema mengikuti sistem": mode mengikuti pengaturan gelap HP. */
    var followSystem by mutableStateOf(false)

    /** Mode yang sedang tampil. Layar ikut berubah begitu nilainya diganti. */
    var dark by mutableStateOf(false)
}

/**
 * Warna tema. Warna utama dipakai sebagai bilah atas. Bawaannya abu-abu tua seperti MT Manager
 * (terang: #2F2F2F, gelap: #141414). User bisa memilih warna lain di Preferensi > Warna Tema.
 */
object ThemeColors {
    val DEFAULT: Int = 0xFF2F2F2F.toInt()

    /** Pilihan warna di dialog Warna Tema. Yang pertama adalah warna bawaan. */
    val OPTIONS: List<Int> = listOf(
        0xFF2F2F2F, 0xFF409EFF, 0xFF5DBB7A, 0xFFE5A23D, 0xFFF56C6C, 0xFF8A9583, 0xFF606266, 0xFF303133,
        0xFFFF5E9E, 0xFFFF0000, 0xFF9B1FFF, 0xFF002FA7, 0xFF81D8CF, 0xFF003152, 0xFF6667AB, 0xFF008982,
        0xFFC30D2E, 0xFFAB5621, 0xFF470024, 0xFF4C0009, 0xFFE95A20, 0xFFFBD26A,
    ).map { it.toInt() }

    /** Warna utama yang sedang dipakai. Layar ikut berubah begitu nilainya diganti. */
    var primary by mutableStateOf(DEFAULT)
}

/** Menggelapkan warna: 1 berarti tetap, 0 berarti hitam. */
private fun darken(color: Color, factor: Float): Color =
    Color(color.red * factor, color.green * factor, color.blue * factor)

/** Warna biru tombol dan sakelar, seperti MT Manager. */
private val BlueAccent = Color(0xFF1774D1)

/** Bilah atas: warna tema. Di mode gelap warna bawaan memakai #141414 seperti MT Manager. */
val Primary: Color
    get() = if (ThemeMode.dark && ThemeColors.primary == ThemeColors.DEFAULT) Color(0xFF141414)
    else Color(ThemeColors.primary)

val PrimaryDark: Color get() = darken(Primary, 0.7f)       // versi lebih gelap dari warna utama

// Tombol teks, judul bagian, sakelar. Warna bawaan memakai biru MT Manager. Jika warna tema diganti,
// warnanya ikut dari warna tema itu, dibuat cukup gelap supaya tetap terbaca di latar putih.
val Accent: Color
    get() {
        if (ThemeColors.primary == ThemeColors.DEFAULT) return BlueAccent
        val base = darken(Primary, 0.75f)
        return if (!ThemeMode.dark && base.luminance() > 0.35f) darken(base, 0.6f) else base
    }

// Ikon folder: abu-abu pada tema bawaan, selain itu mengikuti warna tema
val FolderColor: Color
    get() = when {
        ThemeColors.primary != ThemeColors.DEFAULT -> Primary
        ThemeMode.dark -> Color(0xFFC4C4C4)
        else -> Color(0xFF2F2F2F)
    }

/** Latar halaman utama dan bilah bawah (MT Manager: #FAFAFA terang, #2F2F2F gelap). */
val Body: Color get() = if (ThemeMode.dark) Color(0xFF2F2F2F) else Color(0xFFFAFAFA)

/** Latar kotak dialog, menu, dan kartu (MT Manager: #FFFFFF terang, #424242 gelap). */
val DialogBg: Color get() = if (ThemeMode.dark) Color(0xFF424242) else Color(0xFFFFFFFF)

val SoftGray: Color get() = if (ThemeMode.dark) Color(0xFF3A3A3A) else Color(0xFFF2F2F2)      // latar kotak lembut
val SelectTint: Color get() = if (ThemeMode.dark) Color(0xFF4A4A4A) else Color(0xFFE4E4E4)    // baris yang dipilih
val IconBox: Color get() = if (ThemeMode.dark) Color(0xFF3B3B3B) else Color(0xFFEDEDED)       // kotak di belakang ikon
val PageGray: Color get() = if (ThemeMode.dark) Color(0xFF2F2F2F) else Color(0xFFFAFAFA)      // latar halaman pengaturan
val Ink: Color get() = if (ThemeMode.dark) Color(0xFFC4C4C4) else Color(0xFF2B2B2B)            // teks utama
val Muted: Color get() = if (ThemeMode.dark) Color(0xFF9E9E9E) else Color(0xFF6E6E6E)         // teks kecil, ikon bilah bawah
val Disabled: Color get() = if (ThemeMode.dark) Color(0xFF5C5C5C) else Color(0xFFC8C8C8)      // ikon yang tidak aktif
val LineColor: Color get() = if (ThemeMode.dark) Color(0xFF3E3E3E) else Color(0xFFE3E3E3)     // garis pemisah
val Danger: Color get() = if (ThemeMode.dark) Color(0xFFFF6B6B) else Color(0xFFC62828)        // hapus
val FileTint: Color get() = if (ThemeMode.dark) Color(0xFFB0B0B0) else Color(0xFF757575)      // ikon berkas biasa
val MenuIcon: Color get() = if (ThemeMode.dark) Color(0xFFD0D0D0) else Color(0xFF424242)      // ikon di menu

val HeaderBrush: Brush get() = SolidColor(Primary)

/** Skema warna Material yang mengikuti token di atas, dibuat ulang setiap mode atau warna tema berganti. */
private fun appColors(dark: Boolean) = if (dark) darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = SelectTint,
    onPrimaryContainer = Ink,
    secondary = Accent,
    secondaryContainer = SelectTint,
    onSecondaryContainer = Ink,
    surfaceVariant = SoftGray,
    onSurfaceVariant = Muted,
    outline = Muted,
    background = Body,
    onBackground = Ink,
    surface = DialogBg,
    onSurface = Ink,
    surfaceContainerHigh = DialogBg,
    surfaceContainer = DialogBg,
    surfaceContainerLow = DialogBg,
    error = Danger,
) else lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = SelectTint,
    onPrimaryContainer = Ink,
    secondary = Accent,
    secondaryContainer = SelectTint,
    onSecondaryContainer = Ink,
    surfaceVariant = SoftGray,
    onSurfaceVariant = Muted,
    outline = Muted,
    background = Body,
    onBackground = Ink,
    surface = DialogBg,
    onSurface = Ink,
    surfaceContainerHigh = DialogBg,
    surfaceContainer = DialogBg,
    surfaceContainerLow = DialogBg,
    error = Danger,
)

/**
 * Tema aplikasi. Font memakai font sistem HP. Mode terang/gelap dipilih dari [ThemeMode]:
 * jika "Tema mengikuti sistem" aktif, mode mengikuti pengaturan HP.
 */
@Composable
fun FilemateTheme(content: @Composable () -> Unit) {
    val systemDark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val want = if (ThemeMode.followSystem) systemDark else ThemeMode.manualDark
    if (ThemeMode.dark != want) ThemeMode.dark = want
    // Skema warna dibuat ulang setiap mode atau warna tema berganti
    val colors = remember(ThemeColors.primary, want) { appColors(want) }
    MaterialTheme(colorScheme = colors, content = content)
}
