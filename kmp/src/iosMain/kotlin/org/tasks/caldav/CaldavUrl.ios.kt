package org.tasks.caldav

import platform.Foundation.NSURLComponents

internal actual fun String.toAsciiHost(): String =
    NSURLComponents().also { it.host = this }.encodedHost ?: this
