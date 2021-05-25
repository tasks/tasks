package org.tasks.extensions

import android.view.View

object View {
    fun View.lightStatusBar(isDark: Boolean) {
        systemUiVisibility = if (isDark) {
            systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }
}