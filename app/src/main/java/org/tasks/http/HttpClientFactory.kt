package org.tasks.http

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.DebugNetworkInterceptor
import org.tasks.preferences.Preferences
import org.tasks.security.KeyStoreEncryption
import javax.inject.Inject
import javax.net.ssl.SSLContext

class HttpClientFactory @Inject constructor(
        @ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor,
        private val encryption: KeyStoreEncryption,
        private val cookieJar: CookieJar,
) {
    suspend fun newClient(
        foreground: Boolean = false,
        username: String? = null,
        encryptedPassword: String? = null
    ): OkHttpClient {
        val decrypted = encryptedPassword?.let { encryption.decrypt(it) }
        return newClient(foreground = foreground) { builder ->
            if (!username.isNullOrBlank() && !decrypted.isNullOrBlank()) {
                val auth = BasicDigestAuthHandler(null, username, decrypted)
                builder.addNetworkInterceptor(auth)
                builder.authenticator(auth)
            }
        }
    }

    suspend fun newClient(
        foreground: Boolean = false,
        block: (OkHttpClient.Builder) -> Unit = {}
    ): OkHttpClient {
        val customCertManager = withContext(Dispatchers.Default) {
            CustomCertManager(context)
        }
        customCertManager.appInForeground = foreground
        val hostnameVerifier = customCertManager.hostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient()
            .newBuilder()
            .followRedirects(false)
            .followSslRedirects(true)
            .sslSocketFactory(sslContext.socketFactory, customCertManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(UserAgentInterceptor)
            .cookieJar(cookieJar)

        block(builder)

        if (preferences.isFlipperEnabled) {
            interceptor.apply(builder)
        }
        return builder.build()
    }
}