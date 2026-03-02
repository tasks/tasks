package org.tasks.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.palettes.TonalPalette

fun darkModeColor(seedColor: Int, tone: Int = 80): Int =
    TonalPalette.fromInt(seedColor).tone(tone)

data class ChipColors(val backgroundColor: Int, val contentColor: Int)

@Composable
fun chipColors(seedColor: Int): ChipColors {
    val isDark = isSystemInDarkTheme()
    return remember(seedColor, isDark) { chipColors(seedColor, isDark) }
}

fun chipColors(seedColor: Int, isDark: Boolean): ChipColors {
    val palette = TonalPalette.fromInt(seedColor)
    return if (isDark) {
        ChipColors(palette.tone(30), palette.tone(90))
    } else {
        ChipColors(palette.tone(90), palette.tone(10))
    }
}
