package org.tasks.themes

import android.content.Context
import androidx.annotation.ColorInt
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.kmp.org.tasks.themes.ColorProvider.BLACK
import org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor
import javax.inject.Inject

class ColorProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val isDark = context.resources.getBoolean(R.bool.is_dark)
    private val presetColors: Set<Int> by lazy {
        ThemeColor.COLORS.map { context.getColor(it) }.toSet() - WHITE
    }

    private fun getColor(
        @ColorInt color: Int,
        adjust: Boolean,
        lightTone: Int = ColorTone.LIGHT_TITLE,
        darkTone: Int = ColorTone.DARK_TITLE,
    ) = when {
            adjust && color in presetColors && isDark -> tonalColor(color, darkTone)
            adjust && color in presetColors && !isDark -> tonalColor(color, lightTone)
            !isDark && color == WHITE -> BLACK
            else -> color
        }

    fun isPresetColor(@ColorInt color: Int) = color in presetColors

    fun getThemeColor(@ColorInt color: Int, adjust: Boolean = true) =
            ThemeColor(context, color, getColor(color, adjust))

    fun getChipColor(@ColorInt color: Int, adjust: Boolean = true) =
            ThemeColor(context, color, getColor(color, adjust, ColorTone.LIGHT_CHIP, ColorTone.DARK_CHIP))

    fun getPriorityColor(priority: Int) = priorityColor(
        priority = priority,
        isDarkMode = isDark,
    )

    fun getThemeColors(adjust: Boolean = true) = ThemeColor.COLORS.map { c ->
        getThemeColor(context.getColor(c), adjust)
    }

    fun getWidgetColors() = getThemeColors(false)
}
