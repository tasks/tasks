package org.tasks.themes

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import javax.inject.Inject

class Theme @Inject constructor(
    val themeBase: ThemeBase,
    val themeColor: ThemeColor,
) {
    fun getLayoutInflater(context: Context) =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun applyThemeAndStatusBarColor(activity: Activity) {
        applyTheme(activity)
        themeColor.applyToNavigationBar(activity)
    }

    fun applyTheme(activity: Activity) {
        themeBase.set(activity)
    }
}