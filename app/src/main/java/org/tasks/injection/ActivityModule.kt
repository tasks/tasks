package org.tasks.injection

import android.app.Activity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.ThemeAccent
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeColor

@Module
@InstallIn(ActivityComponent::class)
class ActivityModule {

    @Provides
    @ActivityScoped
    fun getThemeBase(activity: Activity, preferences: Preferences, inventory: Inventory): ThemeBase
            = ThemeBase.getThemeBase(preferences, inventory, activity.intent)

    @Provides
    @ActivityScoped
    fun getThemeColor(colorProvider: ColorProvider, preferences: Preferences): ThemeColor
            = colorProvider.getThemeColor(preferences.defaultThemeColor, true)

    @Provides
    @ActivityScoped
    fun getThemeAccent(colorProvider: ColorProvider, preferences: Preferences): ThemeAccent
            = colorProvider.getThemeAccent(preferences.getInt(R.string.p_theme_accent, 1))
}