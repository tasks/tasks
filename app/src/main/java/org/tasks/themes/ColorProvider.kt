package org.tasks.themes

import android.content.Context
import androidx.annotation.ColorInt
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.kmp.org.tasks.themes.ColorProvider as KmpColorProvider
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor
import javax.inject.Inject

class ColorProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val isDark = context.resources.getBoolean(R.bool.is_dark)

    fun isPresetColor(@ColorInt color: Int) = KmpColorProvider.isPresetColor(color)

    fun getThemeColor(@ColorInt color: Int, adjust: Boolean = true) =
        ThemeColor(
            context,
            color,
            KmpColorProvider.getColor(color, isDark, adjust),
        )

    fun getChipColor(@ColorInt color: Int, adjust: Boolean = true) =
        ThemeColor(
            context,
            color,
            KmpColorProvider.getColor(color, isDark, adjust, ColorTone.LIGHT_CHIP, ColorTone.DARK_CHIP),
        )

    fun getPriorityColor(priority: Int) = priorityColor(
        priority = priority,
        isDarkMode = isDark,
    )

    fun getThemeColors(adjust: Boolean = true) =
        (KmpColorProvider.PRESET_COLORS + KmpColorProvider.WHITE).map { c ->
            getThemeColor(c, adjust)
        }

    fun getWidgetColors() = getThemeColors(false)
}
