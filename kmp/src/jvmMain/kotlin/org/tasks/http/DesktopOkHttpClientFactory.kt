package org.tasks.http

import at.bitfire.cert4android.CertStore
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.cert4android.SettingsProvider
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.TasksBuildConfig
import javax.net.ssl.SSLContext

class DesktopOkHttpClientFactory(
    private val certStore: CertStore,
) : OkHttpClientFactory {
    private val userAgent = "org.tasks/${TasksBuildConfig.VERSION_NAME} (desktop) ${System.getProperty("os.name")}/${System.getProperty("os.version")}"

    override suspend fun newClient(
        foreground: Boolean,
        cookieKey: String?,
        block: (OkHttpClient.Builder) -> Unit,
    ): OkHttpClient {
        val customCertManager = CustomCertManager(
            certStore = certStore,
            settings = object : SettingsProvider {
                override val appInForeground = true
                override val trustSystemCerts = true
            }
        )
        val hostnameVerifier = customCertManager.HostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.socketFactory, customCertManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(UserAgentInterceptor(userAgent))
        block(builder)
        return builder.build()
    }
}
