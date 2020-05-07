package org.tasks.injection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tasks.locale.Locale
import org.tasks.themes.Theme
import org.tasks.themes.ThemeColor
import javax.inject.Inject

abstract class ThemedInjectingAppCompatActivity protected constructor() : AppCompatActivity(), InjectingActivity {
    @Inject lateinit var tasksTheme: Theme
    @Inject protected lateinit var themeColor: ThemeColor

    override lateinit var component: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        component = (application as InjectingApplication).component.plus(ActivityModule(this))
        inject(component)
        title = null
        tasksTheme.applyThemeAndStatusBarColor(this)
        super.onCreate(savedInstanceState)
    }

    init {
        Locale.getInstance(this).applyOverrideConfiguration(this)
    }
}