package org.tasks.http

import at.bitfire.cert4android.CertStore
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.cert4android.SettingsProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.TasksBuildConfig
import org.tasks.security.KeyStoreEncryption
import java.io.File
import javax.net.ssl.SSLContext

class DesktopOkHttpClientFactory(
    private val certStore: CertStore,
    private val encryption: KeyStoreEncryption,
    private val cookieDir: File,
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
            .cookieJar(lazyCookieJar(cookieKey))
        block(builder)
        return builder.build()
    }

    private fun lazyCookieJar(cookieKey: String?): CookieJar = object : CookieJar {
        private val delegate: CookieJar by lazy {
            runBlocking {
                EncryptedCookieStore.getOrCreate(
                    file = EncryptedCookieStore.cookieFile(cookieDir, cookieKey),
                    encryption = encryption,
                )
            }
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) =
            delegate.saveFromResponse(url, cookies)

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            delegate.loadForRequest(url)
    }
}
