package org.tasks.caldav

import java.net.IDN

internal actual fun String.toAsciiHost(): String =
    try {
        IDN.toASCII(this)
    } catch (_: IllegalArgumentException) {
        this
    }
