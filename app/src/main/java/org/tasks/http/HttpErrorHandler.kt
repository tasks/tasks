package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey

open class NetworkException(cause: Throwable? = null) : Exception(cause)
class UnauthorizedException(cause: Throwable? = null) : NetworkException(cause)
class NotFoundException(cause: Throwable? = null) : NetworkException(cause)
class ServiceUnavailableException(cause: Throwable? = null) : NetworkException(cause)
class HttpException(val code: Int, override val message: String? = null) : NetworkException()

class HttpErrorHandler {
    class Config {
        var handleError: suspend (HttpResponse) -> Unit = {}
    }

    companion object Plugin : HttpClientPlugin<Config, HttpErrorHandler> {
        override val key = AttributeKey<HttpErrorHandler>("HttpErrorHandler")

        override fun prepare(block: Config.() -> Unit): HttpErrorHandler {
            val config = Config().apply(block)
            return HttpErrorHandler()
        }

        override fun install(plugin: HttpErrorHandler, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { request ->
                val originalCall = execute(request)

                if (!originalCall.response.status.isSuccess()) {
                    when (originalCall.response.status.value) {
                        401 -> throw UnauthorizedException()
                        404 -> throw NotFoundException()
                        in 500..599 -> throw ServiceUnavailableException()
                        else -> throw HttpException(originalCall.response.status.value)
                    }
                }

                originalCall
            }
        }
    }
}
