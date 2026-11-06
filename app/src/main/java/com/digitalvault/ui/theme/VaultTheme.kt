package com.digitalvault.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4C8DFF), onPrimary = Color.White,
    background = Color(0xFF0F1B33), onBackground = Color(0xFFE8EEF9),
    surface = Color(0xFF17284A), onSurface = Color(0xFFE8EEF9),
    surfaceVariant = Color(0xFF1E3159), onSurfaceVariant = Color(0xFF8FA3C7),
    error = Color(0xFFFF7A7A), outline = Color(0xFF3A5080)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F5FD6), onPrimary = Color.White,
    background = Color(0xFFF4F7FC), onBackground = Color(0xFF0F1B33),
    surface = Color.White, onSurface = Color(0xFF0F1B33),
    surfaceVariant = Color(0xFFE6ECF7), onSurfaceVariant = Color(0xFF55678A),
    error = Color(0xFFC62828), outline = Color(0xFFB5C2DA)
)

@Composable
fun VaultTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
