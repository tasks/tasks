package org.tasks.injection

import android.app.Activity
import android.util.TypedValue
import com.google.android.material.color.DynamicColors
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import org.tasks.billing.Inventory
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
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
    fun getThemeColor(
        colorProvider: ColorProvider,
        preferences: Preferences,
        activity: Activity
    ): ThemeColor {
        return if (preferences.dynamicColor && DynamicColors.isDynamicColorAvailable()) {
            val context = DynamicColors.wrapContextIfAvailable(activity)
            val typedValue = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            val dynamicColor = typedValue.data
            ThemeColor(context, dynamicColor)
        } else {
            colorProvider.getThemeColor(preferences.defaultThemeColor, true)
        }
    }
}
