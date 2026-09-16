package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

interface KtorClientFactory {
    suspend fun newClient(
        foreground: Boolean = false,
        cookieKey: String? = null,
        block: HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient
}
