package com.filemate.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Warna dari desain yang sudah disetujui
val Indigo900 = Color(0xFF3A1C8C)
val Blue700 = Color(0xFF2440B0)
val Cyan500 = Color(0xFF06C8E6)
val Lavender50 = Color(0xFFF5F3FF)
val Lavender100 = Color(0xFFEDE9FE)
val Blue100 = Color(0xFFE0E7FF)
val Blue200 = Color(0xFFC7D2FE)
val Ink = Color(0xFF1E1B4B)
val Muted = Color(0xFF4B5563)
val LineColor = Color(0xFFEEF2FF)
val Danger = Color(0xFFB91C1C)
val FolderTint = Color(0xFF4F46E5)
val BadgeGray = Color(0xFFF3F4F6)
val BadgeText = Color(0xFF374151)

val HeaderGradient = Brush.horizontalGradient(listOf(Indigo900, Blue700))

private val LightColors = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Ink,
    secondary = Indigo900,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    error = Danger,
)

@Composable
fun FilemateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
