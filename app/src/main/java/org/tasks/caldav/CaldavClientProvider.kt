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
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.data.CaldavAccount
import org.tasks.http.UserAgentInterceptor
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
    private val tasksUrl = context.getString(R.string.tasks_caldav_url)

    suspend fun forUrl(
            url: String?,
            username: String? = null,
            password: String? = null
    ): CaldavClient {
        val auth = getAuthInterceptor(username, password, url)
        val customCertManager = newCertManager()
        customCertManager.appInForeground = true
        return CaldavClient(
                this,
                createHttpClient(auth, customCertManager),
                url?.toHttpUrlOrNull()
        )
    }

    suspend fun forTasksAccount(account: CaldavAccount): TasksClient {
        if (!account.isTasksOrg) {
            throw IllegalArgumentException()
        }
        return forAccount(account) as TasksClient
    }

    suspend fun forAccount(account: CaldavAccount, url: String? = account.url): CaldavClient {
        val auth = getAuthInterceptor(
                account.username,
                account.getPassword(encryption),
                account.url
        )
        val customCertManager = newCertManager()
        val client = createHttpClient(auth, customCertManager)
        return if (account.isTasksOrg) {
            TasksClient(this, client, url?.toHttpUrlOrNull())
        } else {
            CaldavClient(this, client, url?.toHttpUrlOrNull())
        }
    }

    private suspend fun newCertManager() = withContext(Dispatchers.Default) {
        CustomCertManager(context)
    }

    private fun getAuthInterceptor(
            username: String?,
            password: String?,
            url: String?
    ): Interceptor? = when {
        username.isNullOrBlank() || password.isNullOrBlank() -> null
        url?.startsWith(tasksUrl) == true -> TasksBasicAuth(username, password, inventory)
        else -> BasicDigestAuthHandler(null, username, password)
    }

    private fun createHttpClient(auth: Interceptor?, customCertManager: CustomCertManager): OkHttpClient {
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
                .addNetworkInterceptor(UserAgentInterceptor)
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