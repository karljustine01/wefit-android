package com.wefit.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF0B0F14),
    primaryContainer = Color(0xFF1F2B14),
    onPrimaryContainer = NeonGreen,
    secondary = TextSecondary,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NavyOutline,
    error = StatusError
)

private val LightColors = lightColorScheme(
    primary = NeonGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4FFC2),
    onPrimaryContainer = Color(0xFF17330A),
    secondary = Color(0xFF55606B),
    background = Color(0xFFF7F8F6),
    onBackground = Color(0xFF14181C),
    surface = Color.White,
    onSurface = Color(0xFF14181C),
    surfaceVariant = Color(0xFFEDEFEA),
    onSurfaceVariant = Color(0xFF55606B),
    outline = Color(0xFFD8DBD5),
    error = StatusError
)

@Composable
fun WeFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // This was previously hardcoded to always use DarkColors, ignoring
    // darkTheme — that was the bug. Now it actually switches.
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = WeFitTypography, content = content)
}