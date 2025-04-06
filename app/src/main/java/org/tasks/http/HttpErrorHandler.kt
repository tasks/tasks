package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

@Serializable
data class GraphErrorResponse(
    val error: GraphError
)

@Serializable
data class GraphError(
    val code: String,
    val message: String,
    val innerError: GraphInnerError? = null
) {
    fun isTokenError() = code in listOf(
        "InvalidAuthenticationToken",
        "AuthenticationError",
        "UnknownError"
    )
}

@Serializable
data class GraphInnerError(
    val date: String,
    @SerialName("request-id") val requestId: String,
    @SerialName("client-request-id")val clientRequestId: String
)

open class NetworkException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
class UnauthorizedException(message: String? = null, cause: Throwable? = null) : NetworkException(message, cause)
class NotFoundException(message: String? = null, cause: Throwable? = null) : NetworkException(message, cause)
class ServiceUnavailableException(message: String? = null, cause: Throwable? = null) : NetworkException(message, cause)
class HttpException(val code: Int, override val message: String? = null) : NetworkException(message)

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
                    val errorResponse = try {
                        originalCall.response.body<GraphErrorResponse>()
                    } catch (e: Exception) {
                        Timber.e(e)
                        null
                    }

                    val errorMessage = buildString {
                        append("HTTP ${originalCall.response.status.value}")
                        errorResponse?.error?.let { error ->
                            append(" - ${error.code}")
                            if (error.message.isNotBlank()) {
                                append(": ${error.message}")
                            }
                            error.innerError?.let { inner ->
                                append(" (Request ID: ${inner.requestId})")
                            }
                        }
                    }

                    Timber.e(errorMessage)

                    when {
                        errorResponse?.error?.isTokenError() == true -> throw UnauthorizedException(errorMessage)
                        originalCall.response.status.value == 404 -> throw NotFoundException(errorMessage)
                        originalCall.response.status.value in 500..599 -> throw ServiceUnavailableException(errorMessage)
                        else -> throw HttpException(originalCall.response.status.value, errorMessage)
                    }
                }

                originalCall
            }
        }
    }
}
