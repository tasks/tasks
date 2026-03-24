package org.tasks.http

import okhttp3.OkHttpClient

class DefaultOkHttpClientFactory : OkHttpClientFactory {
    override suspend fun newClient(
        foreground: Boolean,
        cookieKey: String?,
        block: (OkHttpClient.Builder) -> Unit,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(true)
        block(builder)
        return builder.build()
    }
}
