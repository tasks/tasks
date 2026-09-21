package org.tasks.extensions

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import org.tasks.extensions.Context.hideKeyboard

fun Activity.hideKeyboard() {
    currentFocus?.let {
        hideKeyboard(it)
    }
}

fun Activity.hideKeyboardThen(action: () -> Unit) {
    val decorView = window.decorView
    val ime = WindowInsetsCompat.Type.ime()
    val imeVisible = ViewCompat.getRootWindowInsets(decorView)?.isVisible(ime) == true
    hideKeyboard()
    if (!imeVisible) {
        action()
        return
    }
    var done = false
    fun finish() {
        if (done) return
        done = true
        ViewCompat.setWindowInsetsAnimationCallback(decorView, null)
        action()
    }
    if (Build.VERSION.SDK_INT >= 30) {
        ViewCompat.setWindowInsetsAnimationCallback(
            decorView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ) = insets

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (ViewCompat.getRootWindowInsets(decorView)?.isVisible(ime) != true) {
                        finish()
                    }
                }
            }
        )
    }
    decorView.postDelayed({ finish() }, 350)
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