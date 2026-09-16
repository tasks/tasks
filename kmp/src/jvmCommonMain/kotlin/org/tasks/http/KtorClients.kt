package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient

fun OkHttpClient.toKtor(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient(OkHttp) {
    engine {
        preconfigured = this@toKtor
    }
    block()
}

class OkHttpKtorClientFactory(private val okHttpClientFactory: OkHttpClientFactory) : KtorClientFactory {
    override suspend fun newClient(
        foreground: Boolean,
        cookieKey: String?,
        block: HttpClientConfig<*>.() -> Unit,
    ): HttpClient = okHttpClientFactory.newClient(foreground, cookieKey).toKtor(block)
}
