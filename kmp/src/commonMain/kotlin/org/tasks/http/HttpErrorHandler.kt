package org.tasks.http

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GraphErrorResponse(
    val error: GraphError
)

@Serializable
data class GraphError(
    val code: String,
    val message: String? = null,
    val innerError: GraphInnerError? = null
) {
    fun describe() = buildString {
        append(code)
        if (!message.isNullOrBlank()) {
            append(": $message")
        }
        innerError?.requestId?.let { append(" (Request ID: $it)") }
    }

    fun isTokenError() = code in listOf(
        "InvalidAuthenticationToken",
        "AuthenticationError",
        "UnknownError"
    )
}

@Serializable
data class GraphInnerError(
    val date: String? = null,
    @SerialName("request-id") val requestId: String? = null,
    @SerialName("client-request-id") val clientRequestId: String? = null,
)

open class NetworkException(message: String? = null, cause: Throwable? = null, val graphCode: String? = null) : Exception(message, cause)
class UnauthorizedException(message: String? = null, cause: Throwable? = null, graphCode: String? = null, val status: Int? = null) : NetworkException(message, cause, graphCode)
class NotFoundException(message: String? = null, cause: Throwable? = null, graphCode: String? = null) : NetworkException(message, cause, graphCode)
class ServiceUnavailableException(message: String? = null, cause: Throwable? = null, graphCode: String? = null) : NetworkException(message, cause, graphCode)
class HttpException(val code: Int, override val message: String? = null, graphCode: String? = null, cause: Throwable? = null) : NetworkException(message, cause, graphCode)
class GraphStreamException(message: String, cause: Throwable) : Exception(message, cause)
class ConnectionException(message: String? = null, cause: Throwable? = null) : NetworkException(message, cause)

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
                        Logger.e(e) { "Failed to parse error response" }
                        null
                    }

                    val errorMessage = buildString {
                        append("HTTP ${originalCall.response.status.value}")
                        errorResponse?.error?.let { append(" - ${it.describe()}") }
                    }

                    Logger.e { errorMessage }

                    val graphCode = errorResponse?.error?.code
                    val status = originalCall.response.status.value
                    when {
                        status == 404 -> throw NotFoundException(errorMessage, graphCode = graphCode)
                        status in 500..599 -> throw ServiceUnavailableException(errorMessage, graphCode = graphCode)
                        errorResponse?.error?.isTokenError() == true -> throw UnauthorizedException(errorMessage, graphCode = graphCode, status = status)
                        else -> throw HttpException(status, errorMessage, graphCode = graphCode)
                    }
                }

                originalCall
            }
        }
    }
}

suspend inline fun <reified T> HttpResponse.graphBody(): T =
    try {
        body()
    } catch (e: JsonConvertException) {
        throw inStreamError(e)
    }

suspend fun HttpResponse.inStreamError(cause: JsonConvertException): Exception {
    val text = bodyAsText()
    val error = text.lastIndexOf("{\"error\":")
        .takeIf { it >= 0 }
        ?.let { runCatching { inStreamJson.decodeFromString<GraphErrorResponse>(text.substring(it)) }.getOrNull() }
        ?.error
        ?: return cause
    return GraphStreamException(error.describe(), cause)
}

private val inStreamJson = Json { ignoreUnknownKeys = true }
