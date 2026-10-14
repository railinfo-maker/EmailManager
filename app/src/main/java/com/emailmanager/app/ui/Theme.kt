package com.emailmanager.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlueWhiteColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = Color(0xFF0B2E63),
    secondary = Color(0xFF3F7FD8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EEFF),
    onSecondaryContainer = Color(0xFF0B2E63),
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFFF6F9FF),
    onBackground = Color(0xFF111B2B),
    surface = Color.White,
    onSurface = Color(0xFF111B2B),
    surfaceVariant = Color(0xFFE9F0FB),
    onSurfaceVariant = Color(0xFF44546A),
    surfaceTint = Color(0xFF1565C0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F7FE),
    surfaceContainer = Color(0xFFEDF3FC),
    surfaceContainerHigh = Color(0xFFE8F0FB),
    surfaceContainerHighest = Color(0xFFE2ECF9),
    outline = Color(0xFF8FA3BF),
    outlineVariant = Color(0xFFCAD6E8),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun EmailManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BlueWhiteColors, content = content)
}
