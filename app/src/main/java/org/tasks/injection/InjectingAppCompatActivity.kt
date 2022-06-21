package org.tasks.injection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class InjectingAppCompatActivity protected constructor() : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
    }
}