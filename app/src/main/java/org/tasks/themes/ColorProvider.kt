package org.tasks.themes

import android.content.Context
import androidx.annotation.ColorInt
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor
import org.tasks.kmp.org.tasks.themes.ColorProvider.saturated
import org.tasks.preferences.Preferences
import javax.inject.Inject

class ColorProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    preferences: Preferences
) {

    private val isDark = context.resources.getBoolean(R.bool.is_dark)
    private val desaturate = preferences.desaturateDarkMode

    private fun getColor(@ColorInt color: Int, adjust: Boolean) =
            if (adjust && isDark && desaturate) {
                saturated[color] ?: color
            } else {
                color
            }

    fun getThemeColor(@ColorInt color: Int, adjust: Boolean = true) =
            ThemeColor(context, color, getColor(color, adjust))

    fun getPriorityColor(priority: Int, adjust: Boolean = true) = priorityColor(
        priority = priority,
        isDarkMode = isDark,
        desaturate = adjust && desaturate,
    )

    fun getThemeAccent(index: Int) = ThemeAccent(context, if (isDark && desaturate) {
        ThemeAccent.ACCENTS_DESATURATED[index]
    } else {
        ThemeAccent.ACCENTS[index]
    })

    fun getThemeColors(adjust: Boolean = true) = ThemeColor.COLORS.map { c ->
        getThemeColor(context.getColor(c), adjust)
    }

    fun getWidgetColors() = getThemeColors(false)

    fun getAccentColors() = ThemeAccent.ACCENTS.indices.map(this@ColorProvider::getThemeAccent)
}