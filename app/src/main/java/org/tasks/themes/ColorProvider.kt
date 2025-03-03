package org.tasks.themes

import android.content.Context
import androidx.annotation.ColorInt
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.kmp.org.tasks.themes.ColorProvider.BLACK
import org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor
import org.tasks.kmp.org.tasks.themes.ColorProvider.saturated
import javax.inject.Inject

class ColorProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val isDark = context.resources.getBoolean(R.bool.is_dark)

    private fun getColor(@ColorInt color: Int, adjust: Boolean) =
        when {
            adjust && isDark -> saturated[color] ?: color
            !isDark && color == WHITE -> BLACK
            else -> color
        }

    fun getThemeColor(@ColorInt color: Int, adjust: Boolean = true) =
            ThemeColor(context, color, getColor(color, adjust))

    fun getPriorityColor(priority: Int) = priorityColor(
        priority = priority,
        isDarkMode = isDark,
    )

    fun getThemeColors(adjust: Boolean = true) = ThemeColor.COLORS.map { c ->
        getThemeColor(context.getColor(c), adjust)
    }

    fun getWidgetColors() = getThemeColors(false)
}