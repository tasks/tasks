package org.tasks.http

import java.net.ConnectException

internal actual fun Exception.translated(): Exception = when (this) {
    is ConnectException -> ConnectionException(message, this)
    is com.etebase.client.exceptions.ConnectionException -> ConnectionException(message, this)
    is com.etebase.client.exceptions.UnauthorizedException -> UnauthorizedException(message, this)
    else -> this
}
