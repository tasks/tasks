package org.tasks.http

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
) {

    suspend fun newCertManager() = withContext(Dispatchers.Default) {
        CustomCertManager(context)
    }

    suspend fun newBuilder(
            foreground: Boolean = false,
            username: String? = null,
            encryptedPassword: String? = null
    ): OkHttpClient.Builder = newBuilder(
            newCertManager(),
            foreground = foreground,
            username = username,
            password = encryptedPassword?.let { encryption.decrypt(it) }
    )

    fun newBuilder(
            customCertManager: CustomCertManager,
            foreground: Boolean = false,
            username: String? = null,
            password: String? = null
    ): OkHttpClient.Builder {
        customCertManager.appInForeground = foreground
        val hostnameVerifier = customCertManager.hostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient()
                .newBuilder()
                .sslSocketFactory(sslContext.socketFactory, customCertManager)
                .hostnameVerifier(hostnameVerifier)
        if (preferences.isFlipperEnabled) {
            interceptor.apply(builder)
        }
        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            val auth = BasicDigestAuthHandler(null, username, password)
            builder.addNetworkInterceptor(auth)
            builder.authenticator(auth)
        }
        return builder
    }
}