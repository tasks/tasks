package org.tasks.auth

import java.awt.EventQueue
import java.awt.Frame

internal fun bringAppToForeground() {
    EventQueue.invokeLater {
        Frame.getFrames().forEach { frame ->
            frame.toFront()
            frame.requestFocus()
        }
    }
}
