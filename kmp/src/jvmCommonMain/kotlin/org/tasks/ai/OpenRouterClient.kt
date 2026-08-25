package org.tasks.ai

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

fun HttpClientConfig<*>.installOpenRouter(
    apiKey: String,
    logLevel: LogLevel = LogLevel.HEADERS,
    log: (String) -> Unit,
) {
    // Non-2xx bodies are read so OpenRouter's own error messages can be surfaced;
    // status mapping lives in OpenRouterService instead of a plugin.
    expectSuccess = false

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }

    defaultRequest {
        header(HttpHeaders.Authorization, "Bearer $apiKey")
        header("HTTP-Referer", "https://tasks.org")
        header("X-Title", "Tasks.org")
    }

    install(HttpTimeout) {
        // Double the Graph client's 30s: model inference is slower than a CRUD call.
        requestTimeoutMillis = 60_000
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) = log(message)
        }
        level = logLevel
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}
