package org.tasks.themes

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamicColorScheme
import org.tasks.kmp.org.tasks.themes.ThemeColor
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.kmp.org.tasks.themes.ColorProvider.BLACK
import org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE

const val BLUE = -14575885

@Composable
fun colorOn(color: Color) = colorOn(color.toArgb())

@Composable
fun colorOn(color: Int) = remember (color) { contentColorFor(color) }

val ThemeColorSpring: SpringSpec<Color> = spring(stiffness = Spring.StiffnessMedium)

private val LocalIsDarkTheme = compositionLocalOf<Boolean?> { null }

private val LocalThemeColor = compositionLocalOf { BLUE }

@Composable
@ReadOnlyComposable
private fun currentThemeColor(): Int = LocalThemeColor.current

@Composable
fun rememberThemeColor(filterTint: Int): ThemeColor {
    val isDark = isDarkTheme()
    val appColor = currentThemeColor()
    return remember(filterTint, isDark, appColor) {
        if (filterTint != 0) {
            ColorProvider.themeColor(seedColor = filterTint, isDark = isDark)
        } else {
            ColorProvider.themeColor(seedColor = appColor, isDark = isDark, adjust = false)
        }
    }
}

@Composable
@ReadOnlyComposable
fun isDarkTheme(): Boolean = LocalIsDarkTheme.current ?: isSystemInDarkTheme()

@Composable
fun isDarkTheme(theme: Int): Boolean = when (theme) {
    BaseTheme.LIGHT -> false
    BaseTheme.BLACK, BaseTheme.DARK, BaseTheme.WALLPAPER -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun TasksTheme(
    theme: Int = BaseTheme.DEFAULT,
    primary: Int = BLUE,
    content: @Composable () -> Unit,
) {
    val isDark = isDarkTheme(theme)
    val seedColor = if (primary == WHITE) BLACK else primary
    val generated = dynamicColorScheme(
        seedColor = Color(seedColor),
        isDark = isDark,
    )
    val colorScheme = when (theme) {
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
    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalIsDarkTheme provides isDark,
            LocalThemeColor provides seedColor,
        ) {
            content()
        }
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
