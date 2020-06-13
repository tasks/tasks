package org.tasks.injection

import android.app.Activity
import android.content.Context
import dagger.Module
import dagger.Provides
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.ThemeAccent
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeColor

@Module
class ActivityModule(@get:Provides val activity: Activity) {

    @get:ActivityContext
    @get:Provides
    val activityContext: Context
        get() = activity

    @Provides
    @ActivityScope
    fun getThemeBase(preferences: Preferences, inventory: Inventory): ThemeBase
            = ThemeBase.getThemeBase(preferences, inventory, activity.intent)

    @Provides
    @ActivityScope
    fun getThemeColor(colorProvider: ColorProvider, preferences: Preferences): ThemeColor
            = colorProvider.getThemeColor(preferences.defaultThemeColor, true)

    @Provides
    @ActivityScope
    fun getThemeAccent(colorProvider: ColorProvider, preferences: Preferences): ThemeAccent
            = colorProvider.getThemeAccent(preferences.getInt(R.string.p_theme_accent, 1))
}