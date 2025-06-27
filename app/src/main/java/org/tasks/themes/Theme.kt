package org.tasks.themes

import android.app.Activity
import androidx.core.graphics.ColorUtils
import javax.inject.Inject

class Theme @Inject constructor(
    val themeBase: ThemeBase,
    val themeColor: ThemeColor,
) {
    fun applyThemeAndStatusBarColor(activity: Activity) {
        applyTheme(activity)
        themeColor.applyToNavigationBar(activity)
    }

    fun applyTheme(activity: Activity) {
        themeBase.set(activity)
    }

    companion object {
        fun isLight(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) > 0.5
        }
    }
}