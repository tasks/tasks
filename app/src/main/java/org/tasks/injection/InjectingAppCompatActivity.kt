package org.tasks.injection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tasks.locale.Locale

abstract class InjectingAppCompatActivity protected constructor() : AppCompatActivity(), InjectingActivity {
    override lateinit var component: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        component = (application as InjectingApplication).component.plus(ActivityModule(this))
        inject(component)
        title = ""
        super.onCreate(savedInstanceState)
    }

    init {
        Locale.getInstance(this).applyOverrideConfiguration(this)
    }
}