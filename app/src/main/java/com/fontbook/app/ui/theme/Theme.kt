package com.fontbook.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// A deliberately neutral, low-chroma palette so the typography stays the focus.
private val LightColors = lightColorScheme(
    primary = Color(0xFF4A4640),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6A655C),
    background = Color(0xFFFBF9F5),
    onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFFBF9F5),
    onSurface = Color(0xFF1C1B19),
    surfaceVariant = Color(0xFFEAE6DE),
    onSurfaceVariant = Color(0xFF49463F),
    outline = Color(0xFFBDB8AD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD6D0C5),
    onPrimary = Color(0xFF2F2B25),
    secondary = Color(0xFFB6B0A6),
    background = Color(0xFF15140F),
    onBackground = Color(0xFFE7E3DA),
    surface = Color(0xFF15140F),
    onSurface = Color(0xFFE7E3DA),
    surfaceVariant = Color(0xFF35322B),
    onSurfaceVariant = Color(0xFFCBC6BB),
    outline = Color(0xFF7C786E),
)

@Composable
fun FontBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
