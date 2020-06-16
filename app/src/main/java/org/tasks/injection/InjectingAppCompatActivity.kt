package org.tasks.injection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tasks.locale.Locale

abstract class InjectingAppCompatActivity protected constructor() : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
    }

    init {
        Locale.getInstance(this).applyOverrideConfiguration(this)
    }
}