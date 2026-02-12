package org.tasks.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.tasks.kmp.org.tasks.themes.ColorProvider.BLACK
import org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE
import org.tasks.kmp.org.tasks.themes.ColorProvider.saturated

const val BLUE = -14575885

private val lightColorScheme = lightColorScheme(
    surface = Color(0xFFF0F0F0),
    background = Color.White,
    surfaceContainerLowest = Color.White,
)

private val darkColorScheme = darkColorScheme(
    surface = Color(0xFF0F1416),
    background = Color(0xFF0F1416),
    surfaceContainerLowest = Color(0xFF1B2023),
)

private val blackColorScheme = darkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color(0xFF121212),
)

private val wallpaperScheme = darkColorScheme.copy(
    background = Color.Transparent,
    surface = Color(0x99000000),
)

@Composable
fun colorOn(color: Color) = colorOn(color.toArgb())

@Composable
fun colorOn(color: Int) = remember (color) { contentColorFor(color) }

@Composable
fun TasksTheme(
    theme: Int = 5,
    primary: Int = BLUE,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (theme) {
        0 -> lightColorScheme
        1 -> blackColorScheme
        2 -> darkColorScheme
        3 -> wallpaperScheme
        else -> if (isSystemInDarkTheme()) darkColorScheme else lightColorScheme
    }
    val desaturated = when {
        isSystemInDarkTheme() -> saturated[primary] ?: primary
        primary == WHITE -> BLACK
        else -> primary
    }
    val colorOnPrimary = colorOn(desaturated)
    MaterialTheme(
        colorScheme = colorScheme.copy(
            primary = Color(desaturated),
            onPrimary = colorOnPrimary,
        ),
    ) {
        content()
    }
}

// Settings screen colors — referenced from ThemeBase.java for window background
const val SETTINGS_SURFACE_LIGHT = 0xFFEFECF6.toInt()
const val SETTINGS_SURFACE_DARK = 0xFF191920.toInt()
private const val SETTINGS_CARD_LIGHT = 0xFFF8F8FE.toInt()
private const val SETTINGS_CARD_DARK = 0xFF2B2B34.toInt()

@Composable
fun TasksSettingsTheme(
    theme: Int = 5,
    primary: Int = BLUE,
    content: @Composable () -> Unit,
) {
    TasksTheme(
        theme = theme,
        primary = primary,
    ) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = Color(if (isDark) SETTINGS_SURFACE_DARK else SETTINGS_SURFACE_LIGHT),
                surfaceContainerLowest = Color(if (isDark) SETTINGS_CARD_DARK else SETTINGS_CARD_LIGHT),
            ),
        ) {
            content()
        }
    }
}
