package org.tasks.http

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
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
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun HttpClientConfig<*>.installMicrosoftGraph(
    cookiesStorage: CookiesStorage,
    logLevel: LogLevel = LogLevel.HEADERS,
    log: (String) -> Unit,
    bearerToken: () -> String,
    refreshToken: (suspend (rejected: String?) -> String?)? = null,
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
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }

    install(HttpCookies) {
        storage = cookiesStorage
    }

    if (refreshToken != null) {
        install(GraphTokenRefresh) {
            refresh = refreshToken
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

private class GraphTokenRefreshConfig {
    lateinit var refresh: suspend (rejected: String?) -> String?
}

private val GraphTokenRefresh = createClientPlugin("GraphTokenRefresh", ::GraphTokenRefreshConfig) {
    val refresh = pluginConfig.refresh
    on(Send) { request ->
        val sent = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
        try {
            proceed(request)
        } catch (e: NetworkException) {
            val rejected = when {
                e is UnauthorizedException && e.status == HttpStatusCode.Unauthorized.value -> sent
                e is HttpException && e.code == HttpStatusCode.Unauthorized.value -> null
                else -> throw e
            }
            val token = refresh(rejected)
            if (token == null || token == sent) {
                throw e
            }
            request.headers[HttpHeaders.Authorization] = "Bearer $token"
            proceed(request)
        }
    }
}
