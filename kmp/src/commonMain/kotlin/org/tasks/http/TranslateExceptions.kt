package org.tasks.http

internal suspend fun <T> translateExceptions(block: suspend () -> T): T = try {
    block()
} catch (e: at.bitfire.dav4jvm.ktor.exception.HttpException) {
    throw HttpException(e.statusCode, e.message, cause = e)
} catch (e: Exception) {
    throw e.translated()
}

internal expect fun Exception.translated(): Exception
