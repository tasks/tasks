package org.tasks.extensions

import android.app.Activity
import org.tasks.extensions.Context.hideKeyboard

fun Activity.hideKeyboard() {
    currentFocus?.let {
        hideKeyboard(it)
    }
}