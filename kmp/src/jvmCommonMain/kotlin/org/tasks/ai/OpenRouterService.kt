package org.tasks.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.tasks.http.HttpException
import org.tasks.http.NotFoundException
import org.tasks.http.RateLimitedException
import org.tasks.http.ServiceUnavailableException
import org.tasks.http.UnauthorizedException

class OpenRouterService(private val client: HttpClient) {

    suspend fun listModels(): List<ModelInfo> =
        client.get("$OPENROUTER_BASE_URL/models").require<ModelsResponse>().data

    suspend fun chat(request: ChatRequest): ChatResponse =
        client
            .post("$OPENROUTER_BASE_URL/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .require()

    private suspend inline fun <reified T> HttpResponse.require(): T {
        if (status.isSuccess()) return body()
        val detail = runCatching { body<OpenRouterError>().error?.message }.getOrNull()
        val message = "HTTP ${status.value}" + (detail?.let { " - $it" } ?: "")
        throw when {
            status.value == 401 || status.value == 403 -> UnauthorizedException(message)
            status.value == 404 -> NotFoundException(message)
            status.value == 429 -> RateLimitedException(
                message,
                headers["Retry-After"]?.toLongOrNull(),
            )
            status.value in 500..599 -> ServiceUnavailableException(message)
            else -> HttpException(status.value, message)
        }
    }
}
