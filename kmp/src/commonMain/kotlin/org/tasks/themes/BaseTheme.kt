package org.tasks.themes

object BaseTheme {
    const val LIGHT = 0
    const val BLACK = 1
    const val DARK = 2
    const val WALLPAPER = 3
    const val DAY_NIGHT = 4
    const val SYSTEM_DEFAULT = 5

    const val DEFAULT = SYSTEM_DEFAULT

    fun isFree(index: Int) = index < WALLPAPER || index == SYSTEM_DEFAULT
}
