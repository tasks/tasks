package org.tasks.http

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
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

fun HttpClientConfig<*>.installTodoist(
    logLevel: LogLevel = LogLevel.HEADERS,
    log: (String) -> Unit,
    bearerToken: () -> String,
) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                useAlternativeNames = true
                explicitNulls = false
                encodeDefaults = true
            }
        )
    }

    defaultRequest {
        header(HttpHeaders.Authorization, "Bearer ${bearerToken()}")
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
    }

    install(HttpRequestRetry) {
        maxRetries = MAX_RETRIES
        retryIf { _, response -> response.status.value == 429 || response.status.value in 500..599 }
        retryOnExceptionIf { _, cause -> cause is kotlinx.io.IOException }
        delayMillis { attempt ->
            response
                ?.headers
                ?.get(HttpHeaders.RetryAfter)
                ?.toLongOrNull()
                ?.times(1_000)
                ?.coerceAtMost(MAX_RETRY_DELAY_MS)
                ?: (1_000L shl (attempt - 1).coerceIn(0, 4))
        }
    }

    install(HttpErrorHandler)

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) = log(message)
        }
        level = logLevel
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}

private const val MAX_RETRIES = 3
private const val MAX_RETRY_DELAY_MS = 60_000L
