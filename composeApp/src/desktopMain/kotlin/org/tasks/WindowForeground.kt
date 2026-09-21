package org.tasks

import co.touchlab.kermit.Logger
import java.awt.Desktop
import java.awt.EventQueue
import java.awt.Frame

private const val TAG = "WindowForeground"

@Volatile
private var quitting = false

fun setQuitting(value: Boolean) {
    quitting = value
}

fun requestForeground() {
    EventQueue.invokeLater {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().requestForeground(true)
            }
        } catch (e: Exception) {
            Logger.d(throwable = e, tag = TAG) { "requestForeground unavailable" }
        }

        appWindows().forEach { frame ->
            if (!frame.isVisible) {
                if (quitting) {
                    return@forEach
                }
                frame.isVisible = true
            }
            frame.extendedState = frame.extendedState and Frame.ICONIFIED.inv()
            frame.toFront()
            frame.requestFocus()
        }
    }
}

private fun appWindows(): List<Frame> =
    Frame.getFrames().filter { it::class.java.name == COMPOSE_WINDOW }

private const val COMPOSE_WINDOW = "androidx.compose.ui.awt.ComposeWindow"
