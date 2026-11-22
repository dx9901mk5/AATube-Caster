package com.aayt.caster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CarDarkColorScheme = darkColorScheme(
    primary = YtRed,
    onPrimary = Color.White,
    primaryContainer = YtRedContainer,
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = Color(0xFF3EA6FF),
    onSecondary = Color.Black,
    background = YtBackground,
    onBackground = YtTextPrimary,
    surface = YtSurface,
    onSurface = YtTextPrimary,
    surfaceVariant = YtSurfaceVariant,
    onSurfaceVariant = YtTextSecondary
)

@Composable
fun AATubeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CarDarkColorScheme,
        typography = Typography,
        content = content
    )
}
