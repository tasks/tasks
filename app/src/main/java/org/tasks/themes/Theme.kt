package org.tasks.themes

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.view.LayoutInflater
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.R
import javax.inject.Inject

class Theme @Inject constructor(
    @ActivityContext val context: Context,
    val themeBase: ThemeBase,
    val themeColor: ThemeColor,
    private val themeAccent: ThemeAccent
) {
    private val darkTheme = themeBase.isDarkTheme(context as Activity)

    @Composable
    fun TasksTheme(
        content: @Composable () -> Unit,
    ) {
        val primary = Color(themeColor.primaryColor)
        val onPrimary = Color(themeColor.colorOnPrimary)
        val secondary = Color(themeAccent.accentColor)
        MaterialTheme(
            colors = if (darkTheme) {
                darkColors(
                    primary = primary,
                    onPrimary = onPrimary,
                    secondary = secondary,
                    background = Color(ContextCompat.getColor(context, R.color.window_background)),
                    surface = Color(ContextCompat.getColor(context, R.color.content_background)),
                )
            } else {
                lightColors(
                    primary = primary,
                    onPrimary = onPrimary,
                    secondary = secondary,
                )
            },
            content = content
        )
    }

    fun withThemeColor(themeColor: ThemeColor) = Theme(context, themeBase, themeColor, themeAccent)

    fun getLayoutInflater(context: Context) =
        wrap(context).getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun applyThemeAndStatusBarColor(activity: Activity) {
        applyTheme(activity)
        themeColor.applyToSystemBars(activity)
        themeColor.applyTaskDescription(activity, activity.getString(R.string.app_name))
    }

    fun applyTheme(activity: Activity) {
        themeBase.set(activity)
        applyToContext(activity)
        activity.window.setFormat(PixelFormat.RGBA_8888)
    }

    fun applyToContext(context: Context) {
        val theme = context.theme
        themeColor.applyStyle(theme)
        themeAccent.applyStyle(theme)
    }

    private fun wrap(context: Context): Context {
        val wrapper = themeBase.wrap(context)
        applyToContext(wrapper)
        return wrapper
    }
}