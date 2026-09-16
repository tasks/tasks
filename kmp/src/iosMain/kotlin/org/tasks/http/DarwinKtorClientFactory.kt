package org.tasks.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.UserAgent
import org.tasks.TasksBuildConfig
import platform.UIKit.UIDevice

class DarwinKtorClientFactory : KtorClientFactory {
    private val userAgent = with(UIDevice.currentDevice) {
        "org.tasks/${TasksBuildConfig.VERSION_NAME} (ios) $systemName/$systemVersion"
    }

    override suspend fun newClient(
        foreground: Boolean,
        cookieKey: String?,
        block: HttpClientConfig<*>.() -> Unit,
    ): HttpClient = HttpClient(Darwin) {
        followRedirects = false
        install(UserAgent) { agent = userAgent }
        block()
    }
}
