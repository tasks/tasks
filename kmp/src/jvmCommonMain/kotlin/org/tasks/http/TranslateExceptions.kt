package org.tasks.http

import java.net.ConnectException

internal suspend fun <T> translateExceptions(block: suspend () -> T): T = try {
    block()
} catch (e: at.bitfire.dav4jvm.ktor.exception.HttpException) {
    throw HttpException(e.statusCode, e.message, cause = e)
} catch (e: ConnectException) {
    throw ConnectionException(e.message, e)
} catch (e: com.etebase.client.exceptions.ConnectionException) {
    throw ConnectionException(e.message, e)
} catch (e: com.etebase.client.exceptions.UnauthorizedException) {
    throw UnauthorizedException(e.message, e)
}
