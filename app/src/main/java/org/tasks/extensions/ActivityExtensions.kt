package org.tasks.extensions

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import org.tasks.extensions.Context.hideKeyboard

fun Activity.hideKeyboard() {
    currentFocus?.let {
        hideKeyboard(it)
    }
}

fun ComponentActivity.addBackPressedCallback(block: () -> Unit) {
    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                block()
            }
        }
    )
}