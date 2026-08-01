package org.tasks.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamicColorScheme
import org.tasks.kmp.org.tasks.themes.ColorProvider.BLACK
import org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE
import java.util.concurrent.ConcurrentHashMap

const val BLUE = -14575885

/**
 * Generating a scheme runs the full HCT tonal palette derivation, which is far too expensive to
 * repeat per composition. There are only a handful of (theme, seed) combinations in practice, and
 * the schemes are immutable, so they're cached for the lifetime of the process.
 *
 * This matters most on the task list, where every row hosts its own composition for the chip row.
 */
private val colorSchemeCache = ConcurrentHashMap<Triple<Int, Int, Boolean>, ColorScheme>()

private fun getColorScheme(theme: Int, seedColor: Int, isDark: Boolean): ColorScheme =
    colorSchemeCache.getOrPut(Triple(theme, seedColor, isDark)) {
        generateColorScheme(theme, seedColor, isDark)
    }

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
    val isDark = when (theme) {
        0 -> false
        1, 2, 3 -> true
        else -> isSystemInDarkTheme()
    }
    val seedColor = if (primary == WHITE) BLACK else primary
    val colorScheme = remember(theme, seedColor, isDark) {
        getColorScheme(theme, seedColor, isDark)
    }
    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}

private fun generateColorScheme(theme: Int, seedColor: Int, isDark: Boolean): ColorScheme {
    val generated = dynamicColorScheme(
        seedColor = Color(seedColor),
        isDark = isDark,
    )
    return when (theme) {
        0 -> generated.copy(
            surface = Color(0xFFF0F0F0),
            background = Color.White,
            surfaceContainerLowest = Color.White,
        )
        1 -> generated.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color(0xFF121212),
        )
        2 -> generated.copy(
            surface = Color(0xFF0F1416),
            background = Color(0xFF0F1416),
            surfaceContainerLowest = Color(0xFF1B2023),
        )
        3 -> generated.copy(
            background = Color.Transparent,
            surface = Color(0x99000000),
        )
        else -> if (isDark) generated.copy(
            surface = Color(0xFF0F1416),
            background = Color(0xFF0F1416),
            surfaceContainerLowest = Color(0xFF1B2023),
        ) else generated.copy(
            surface = Color(0xFFF0F0F0),
            background = Color.White,
            surfaceContainerLowest = Color.White,
        )
    }
}

val WarningColor = Color(0xFFFF9800)

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
