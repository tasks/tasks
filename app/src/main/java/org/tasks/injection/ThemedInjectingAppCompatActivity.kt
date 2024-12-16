package org.tasks.injection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tasks.themes.Theme
import org.tasks.themes.ThemeColor
import javax.inject.Inject

abstract class ThemedInjectingAppCompatActivity protected constructor() : AppCompatActivity() {
    @Inject lateinit var tasksTheme: Theme
    @Inject lateinit var themeColor: ThemeColor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tasksTheme.applyThemeAndStatusBarColor(this)
        title = null
    }
}