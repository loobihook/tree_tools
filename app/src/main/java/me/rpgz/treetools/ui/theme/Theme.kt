package me.rpgz.treetools.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4ade80),
    onPrimary = Color(0xFF064e3b),
    primaryContainer = Color(0xFF064e3b),
    onPrimaryContainer = Color(0xFFa7f3d0),
    secondary = Color(0xFF6ee7b7),
    onSecondary = Color(0xFF065f46),
    secondaryContainer = Color(0xFF065f46),
    onSecondaryContainer = Color(0xFF8ff7d0),
    tertiary = Color(0xFF22d3ee),
    onTertiary = Color(0xFF0369a1),
    tertiaryContainer = Color(0xFF0369a1),
    onTertiaryContainer = Color(0xFFa5f3ff),
    background = Color(0xFF022c22),
    onBackground = Color(0xFFcff7ed),
    surface = Color(0xFF022c22),
    onSurface = Color(0xFFcff7ed),
    surfaceVariant = Color(0xFF145341),
    onSurfaceVariant = Color(0xFF8dd3bc),
    outline = Color(0xFF8dd3bc),
    outlineVariant = Color(0xFF145341),
    error = Color(0xFFfca5a5),
    onError = Color(0xFF7f1d1d),
    errorContainer = Color(0xFF7f1d1d),
    onErrorContainer = Color(0xFFfca5a5),
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF065f46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF86efac),
    onPrimaryContainer = Color(0xFF023f2e),
    secondary = Color(0xFF065f46),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF8ff7d0),
    onSecondaryContainer = Color(0xFF023f2e),
    tertiary = Color(0xFF0369a1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFbae6fd),
    onTertiaryContainer = Color(0xFF001e36),
    background = Color(0xFFf0fdf4),
    onBackground = Color(0xFF022c22),
    surface = Color(0xFFf0fdf4),
    onSurface = Color(0xFF022c22),
    surfaceVariant = Color(0xFFdcebe3),
    onSurfaceVariant = Color(0xFF145341),
    outline = Color(0xFF145341),
    outlineVariant = Color(0xFFdcebe3),
    error = Color(0xFFb91c1c),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFfee2e2),
    onErrorContainer = Color(0xFF450a0a),
    scrim = Color(0xFF000000)
)

@Composable
fun TreeToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}