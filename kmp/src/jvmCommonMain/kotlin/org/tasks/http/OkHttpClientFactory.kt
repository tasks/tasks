package org.tasks.http

import okhttp3.OkHttpClient

interface OkHttpClientFactory {
    suspend fun newClient(
        foreground: Boolean = false,
        cookieKey: String? = null,
        block: (OkHttpClient.Builder) -> Unit = {},
    ): OkHttpClient
}
