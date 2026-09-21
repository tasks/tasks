package org.tasks.extensions

import kotlin.system.exitProcess

actual fun restartApplication() {
    exitProcess(0)
}
