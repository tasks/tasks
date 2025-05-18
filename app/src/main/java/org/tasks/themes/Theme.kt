package org.tasks.themes

import android.app.Activity
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
}