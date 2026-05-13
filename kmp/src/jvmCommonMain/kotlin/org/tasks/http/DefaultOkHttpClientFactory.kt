package org.tasks.http

import okhttp3.OkHttpClient
import org.tasks.TasksBuildConfig

class DefaultOkHttpClientFactory : OkHttpClientFactory {
    private val userAgent = "org.tasks/${TasksBuildConfig.VERSION_NAME} (desktop) ${System.getProperty("os.name")}/${System.getProperty("os.version")}"

    override suspend fun newClient(
        foreground: Boolean,
        cookieKey: String?,
        block: (OkHttpClient.Builder) -> Unit,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(true)
            .addInterceptor(UserAgentInterceptor(userAgent))
        block(builder)
        return builder.build()
    }
}
