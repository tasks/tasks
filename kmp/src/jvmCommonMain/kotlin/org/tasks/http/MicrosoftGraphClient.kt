package org.tasks.http

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun HttpClientConfig<*>.installMicrosoftGraph(
    cookiesStorage: CookiesStorage,
    logLevel: LogLevel = LogLevel.HEADERS,
    log: (String) -> Unit,
    bearerToken: () -> String,
) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }

    defaultRequest {
        header(HttpHeaders.Authorization, "Bearer ${bearerToken()}")
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
    }

    install(HttpCookies) {
        storage = cookiesStorage
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
