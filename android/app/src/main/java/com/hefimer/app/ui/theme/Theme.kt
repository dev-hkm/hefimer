package com.hefimer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HefimerDarkScheme = darkColorScheme(
    primary = HefimerColors.White,
    onPrimary = HefimerColors.Black,
    secondary = HefimerColors.Emerald400,
    onSecondary = HefimerColors.Black,
    background = HefimerColors.Black,
    onBackground = HefimerColors.White,
    surface = HefimerColors.SurfaceCard,
    onSurface = HefimerColors.White,
    surfaceVariant = HefimerColors.SurfaceCardAlt,
    onSurfaceVariant = HefimerColors.White60,
    outline = HefimerColors.White15,
    outlineVariant = HefimerColors.White10,
    error = HefimerColors.Red400,
    onError = HefimerColors.Black,
)

@Composable
fun HefimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HefimerDarkScheme,
        typography = HefimerTypography,
        content = content,
    )
}
