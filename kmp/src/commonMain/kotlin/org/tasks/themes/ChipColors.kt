package org.tasks.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.contrast.Contrast
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette

object ColorTone {
    const val LIGHT_CHIP = -1
    const val DARK_CHIP = 30
    const val LIGHT_CONTENT = 10
    const val DARK_CONTENT = 90
    const val LIGHT_DRAWER = -1
    const val DARK_DRAWER = 80
    const val LIGHT_CHECKBOX = -1
    const val DARK_CHECKBOX = 70
    const val LIGHT_TITLE = -1
    const val DARK_TITLE = 80
    const val LIGHT_APPBAR = 40
    const val DARK_APPBAR = 80
}

fun tonalColor(seedColor: Int, tone: Int): Int =
    when {
        tone < 0 -> seedColor
        else -> TonalPalette.fromInt(seedColor).tone(tone)
    }

data class ChipColors(val backgroundColor: Int, val contentColor: Int)

@Composable
fun chipColors(seedColor: Int): ChipColors {
    val isDark = isSystemInDarkTheme()
    return remember(seedColor, isDark) { chipColors(seedColor, isDark) }
}

fun chipColors(seedColor: Int, isDark: Boolean): ChipColors {
    val bgTone = if (isDark) ColorTone.DARK_CHIP else ColorTone.LIGHT_CHIP
    val bgColor = tonalColor(seedColor, bgTone)
    return ChipColors(bgColor, contentColor(bgColor))
}

private const val MIN_CONTRAST_RATIO = 4.5
private const val WHITE = -1         // 0xFFFFFFFF
private const val BLACK = -16777216  // 0xFF000000

fun contentColor(backgroundColor: Int): Int {
    val bgTone = Hct.fromInt(backgroundColor).tone
    val palette = TonalPalette.fromInt(backgroundColor)
    val lightRatio = Contrast.ratioOfTones(bgTone, ColorTone.LIGHT_CONTENT.toDouble())
    val darkRatio = Contrast.ratioOfTones(bgTone, ColorTone.DARK_CONTENT.toDouble())
    val bestRatio = maxOf(lightRatio, darkRatio)
    if (bestRatio >= MIN_CONTRAST_RATIO) {
        val contentTone = if (lightRatio >= darkRatio) ColorTone.LIGHT_CONTENT else ColorTone.DARK_CONTENT
        return palette.tone(contentTone)
    }
    val whiteRatio = Contrast.ratioOfTones(bgTone, 100.0)
    val blackRatio = Contrast.ratioOfTones(bgTone, 0.0)
    return if (whiteRatio >= blackRatio) WHITE else BLACK
}
