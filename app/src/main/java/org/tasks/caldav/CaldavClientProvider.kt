package org.tasks.caldav

import android.content.Context
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.tasks.DebugNetworkInterceptor
import org.tasks.billing.Inventory
import org.tasks.data.CaldavAccount
import org.tasks.preferences.Preferences
import org.tasks.security.KeyStoreEncryption
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLContext

class CaldavClientProvider @Inject constructor(
        @ApplicationContext private val context: Context,
        private val encryption: KeyStoreEncryption,
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor,
        private val inventory: Inventory
) {
    suspend fun forUrl(
            url: String?,
            username: String? = null,
            password: String? = null,
            token: String? = null): CaldavClient {
        val auth = getAuthInterceptor(username = username, password = password, token = token)
        val customCertManager = newCertManager()
        return CaldavClient(
                this,
                customCertManager,
                createHttpClient(auth, customCertManager),
                url?.toHttpUrlOrNull()
        )
    }

    suspend fun forAccount(account: CaldavAccount, url: String? = account.url): CaldavClient {
        val auth = getAuthInterceptor(account)
        val customCertManager = newCertManager()
        return CaldavClient(
                this,
                customCertManager,
                createHttpClient(auth, customCertManager),
                url?.toHttpUrlOrNull()
        )
    }

    private suspend fun newCertManager() = withContext(Dispatchers.Default) {
        CustomCertManager(context)
    }

    private fun getAuthInterceptor(
            account: CaldavAccount? = null,
            username: String? = account?.username,
            password: String? = account?.getPassword(encryption),
            token: String? = null
    ): Interceptor? {
        return when {
            account?.isTasksOrg == true ->
                account.password
                        ?.let { encryption.decrypt(it) }
                        ?.let { TokenInterceptor(it, inventory) }
            username?.isNotBlank() == true && password?.isNotBlank() == true ->
                BasicDigestAuthHandler(null, username, password)
            token?.isNotBlank() == true ->
                TokenInterceptor(token, inventory)
            else -> null
        }
    }

    private fun createHttpClient(auth: Interceptor?, customCertManager: CustomCertManager, foreground: Boolean = false): OkHttpClient {
        customCertManager.appInForeground = foreground
        val hostnameVerifier = customCertManager.hostnameVerifier(OkHostnameVerifier)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(customCertManager), null)
        val builder = OkHttpClient()
                .newBuilder()
                .cookieJar(MemoryCookieStore())
                .followRedirects(false)
                .followSslRedirects(true)
                .sslSocketFactory(sslContext.socketFactory, customCertManager)
                .hostnameVerifier(hostnameVerifier)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
        auth?.let {
            builder.addNetworkInterceptor(it)
            if (it is Authenticator) {
                builder.authenticator(it)
            }
        }
        if (preferences.isFlipperEnabled) {
            interceptor.apply(builder)
        }

        return builder.build()
    }
}