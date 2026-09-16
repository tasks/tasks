package org.tasks.http

import io.ktor.client.engine.darwin.DarwinHttpRequestException

internal actual fun Exception.translated(): Exception = when (this) {
    is DarwinHttpRequestException -> ConnectionException(message, this)
    else -> this
}
