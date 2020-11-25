package org.tasks.etebase

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import com.etesync.journalmanager.util.TokenAuthenticator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.DebugNetworkInterceptor
import org.tasks.caldav.MemoryCookieStore
import org.tasks.data.CaldavAccount
import org.tasks.preferences.Preferences
import org.tasks.security.KeyStoreEncryption
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLContext

class EteBaseClientProvider @Inject constructor(
        @ApplicationContext private val context: Context,
        private val encryption: KeyStoreEncryption,
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor
) {
    @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
    suspend fun forAccount(account: CaldavAccount): EteBaseClient {
        return forUrl(
                account.url!!,
                account.username,
                account.getEncryptionPassword(encryption),
                account.getPassword(encryption))
    }

    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    suspend fun forUrl(url: String, username: String?, encryptionPassword: String?, token: String?): EteBaseClient = withContext(Dispatchers.IO) {
        val customCertManager = newCertManager()
        EteBaseClient(
                customCertManager,
                username,
                encryptionPassword,
                token,
                createHttpClient(token, customCertManager),
                url.toHttpUrl()
        )
    }

    private suspend fun newCertManager() = withContext(Dispatchers.Default) {
        CustomCertManager(context)
    }

    private fun createHttpClient(
            token: String?,
            customCertManager: CustomCertManager,
            foreground: Boolean = false
    ): OkHttpClient {
        customCertManager.appInForeground = foreground
        val hostnameVerifier = customCertManager.hostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient()
                .newBuilder()
                .addNetworkInterceptor(TokenAuthenticator(null, token))
                .cookieJar(MemoryCookieStore())
                .followRedirects(false)
                .followSslRedirects(true)
                .sslSocketFactory(sslContext.socketFactory, customCertManager)
                .hostnameVerifier(hostnameVerifier)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
        if (preferences.isFlipperEnabled) {
            interceptor.apply(builder)
        }
        return builder.build()
    }
}