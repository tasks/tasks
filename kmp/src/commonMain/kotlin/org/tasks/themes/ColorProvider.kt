package org.tasks.kmp.org.tasks.themes

import org.tasks.data.entity.Task
import org.tasks.themes.ColorTone
import org.tasks.themes.tonalColor

object ColorProvider {
    const val BLUE_500 = -14575885
    private const val RED_500 = -769226
    private const val AMBER_500 = -16121
    private const val GREY_500 = -6381922
    const val WHITE = -1
    const val BLACK = -16777216

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
