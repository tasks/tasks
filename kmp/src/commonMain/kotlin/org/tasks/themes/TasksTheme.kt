package org.tasks.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.tasks.kmp.org.tasks.themes.ColorProvider.BLACK
import org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE
import org.tasks.kmp.org.tasks.themes.ColorProvider.saturated

const val BLUE = -14575885

private val lightColorScheme = lightColorScheme(
    surface = Color(0xFFEAEFF1),
    background = Color.White,
)

private val darkColorScheme = darkColorScheme(
    surface = Color(0xFF1B2023),
    background = Color(0xFF0F1416),
)

private val blackColorScheme = darkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
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
