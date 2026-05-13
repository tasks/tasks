package org.tasks.kmp.org.tasks.themes

import org.tasks.data.entity.Task
import org.tasks.themes.ColorTone
import org.tasks.themes.contentColor
import org.tasks.themes.tonalColor

data class ThemeColor(
    val originalColor: Int,
    val primaryColor: Int,
    val onPrimaryColor: Int,
)

object ColorProvider {
    const val BLUE_500 = -14575885
    private const val RED_500 = -769226
    private const val AMBER_500 = -16121
    private const val GREY_500 = -6381922
    const val WHITE = -1
    const val BLACK = -16777216

    // Preset colors from the color picker (hex values from kmp/src/androidMain/res/values/colors.xml)
    val PRESET_COLORS: Set<Int> = setOf(
        0xFFD50000.toInt(), // tomato
        0xFFF44336.toInt(), // red_500
        0xFFFF5722.toInt(), // deep_orange_500
        0xFFF4511E.toInt(), // tangerine
        0xFFEF6C00.toInt(), // pumpkin
        0xFFFF9800.toInt(), // orange_500
        0xFFF09300.toInt(), // mango
        0xFFF6BF26.toInt(), // banana
        0xFFFFC107.toInt(), // amber_500
        0xFFE4C441.toInt(), // citron
        0xFFFFEB3B.toInt(), // yellow_500
        0xFFCDDC39.toInt(), // lime_500
        0xFFC0CA33.toInt(), // avocado
        0xFF8BC34A.toInt(), // light_green_500
        0xFF7CB342.toInt(), // pistachio
        0xFF4CAF50.toInt(), // green_500
        0xFF0B8043.toInt(), // basil
        0xFF009688.toInt(), // teal_500
        0xFF33B679.toInt(), // sage
        0xFF00BCD4.toInt(), // cyan_500
        0xFF03A9F4.toInt(), // light_blue_500
        0xFF039BE5.toInt(), // peacock
        0xFF2196F3.toInt(), // blue_500
        0xFF4285F4.toInt(), // cobalt
        0xFF3F51B5.toInt(), // indigo_500
        0xFF7986CB.toInt(), // lavender
        0xFFB39DDB.toInt(), // wisteria
        0xFF9E69AF.toInt(), // amethyst
        0xFF673AB7.toInt(), // deep_purple_500
        0xFF8E24AA.toInt(), // grape
        0xFF9C27B0.toInt(), // purple_500
        0xFFAD1457.toInt(), // radicchio
        0xFFE91E63.toInt(), // pink_500
        0xFFD81B60.toInt(), // cherry_blossom
        0xFFE67C73.toInt(), // flamingo
        0xFF795548.toInt(), // brown_500
        0xFF616161.toInt(), // graphite
        0xFFA79B8E.toInt(), // birch
        0xFF9E9E9E.toInt(), // grey_500
        0xFF607D8B.toInt(), // blue_grey_500
    )

    fun themeColor(
        seedColor: Int,
        isDark: Boolean,
        adjust: Boolean = true,
    ): ThemeColor {
        val color = getColor(
            color = seedColor,
            isDark = isDark,
            adjust = adjust,
        )
        return ThemeColor(
            originalColor = seedColor,
            primaryColor = color,
            onPrimaryColor = contentColor(color),
        )
    }

    fun isPresetColor(color: Int) = color in PRESET_COLORS

    fun getColor(
        color: Int,
        isDark: Boolean,
        adjust: Boolean,
        lightTone: Int = ColorTone.LIGHT_TITLE,
        darkTone: Int = ColorTone.DARK_TITLE,
    ): Int = when {
        adjust && color in PRESET_COLORS && isDark -> tonalColor(color, darkTone)
        adjust && color in PRESET_COLORS && !isDark -> tonalColor(color, lightTone)
        !isDark && color == WHITE -> BLACK
        else -> color
    }

    fun priorityColor(priority: Int, isDarkMode: Boolean = false): Int {
        val color = when (priority) {
            in Int.MIN_VALUE..Task.Priority.HIGH -> RED_500
            Task.Priority.MEDIUM -> AMBER_500
            Task.Priority.LOW -> BLUE_500
            else -> GREY_500
        }
        return tonalColor(
            seedColor = color,
            tone = if (isDarkMode) ColorTone.DARK_CHECKBOX else ColorTone.LIGHT_CHECKBOX
        )
    }
}
