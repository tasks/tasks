package org.tasks.caldav

import at.bitfire.dav4jvm.okhttp.BasicDigestAuthHandler
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.entity.CaldavAccount
import org.tasks.fcm.FcmTokenProvider
import org.tasks.http.OkHttpClientFactory
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyStoreEncryption
import java.util.concurrent.TimeUnit

class CaldavClientProvider(
    private val encryption: KeyStoreEncryption,
    private val tasksPreferences: TasksPreferences,
    private val environment: TasksServerEnvironment,
    private val httpClientFactory: OkHttpClientFactory,
    private val tokenProvider: FcmTokenProvider? = null,
    private val subscriptionProvider: () -> TasksBasicAuth.SubscriptionInfo? = { null },
) {

    suspend fun forUrl(
        url: String?,
        username: String?,
        password: String?,
    ): CaldavClient {
        val tosVersion = tasksPreferences.get(TasksPreferences.acceptedTosVersion, 0)
        val auth = getAuthInterceptor(username, password, url, tosVersion)
        val client = createHttpClient(auth = auth, foreground = true)
        return CaldavClient(this, client, url?.toHttpUrlOrNull())
    }

    suspend fun forTasksAccount(account: CaldavAccount): TasksClient {
        if (!account.isTasksOrg) {
            throw IllegalArgumentException()
        }
        return forAccount(account) as TasksClient
    }

    suspend fun forAccount(account: CaldavAccount, url: String? = account.url): CaldavClient {
        val tosVersion = tasksPreferences.get(TasksPreferences.acceptedTosVersion, 0)
        val password = encryption.decrypt(account.password) ?: ""
        val pushToken = if (account.isTasksOrg) tokenProvider?.getToken() else null
        val auth = getAuthInterceptor(
            account.username,
            password,
            account.url,
            tosVersion,
            pushToken,
        )
        val client = createHttpClient(auth)
        return if (account.isTasksOrg) {
            TasksClient(this, client, url?.toHttpUrlOrNull())
        } else {
            CaldavClient(this, client, url?.toHttpUrlOrNull())
        }
    }

    private fun getAuthInterceptor(
        username: String?,
        password: String?,
        url: String?,
        tosVersion: Int,
        pushToken: String? = null,
    ): Interceptor? = when {
        username.isNullOrBlank() || password.isNullOrBlank() -> null
        url?.startsWith(environment.caldavUrl) == true -> TasksBasicAuth(
            user = username,
            token = password,
            tosVersion = tosVersion,
            pushToken = pushToken,
            subscriptionInfo = subscriptionProvider(),
        )
        else -> BasicDigestAuthHandler(null, username, password.toCharArray())
    }

    private suspend fun createHttpClient(
        auth: Interceptor?,
        foreground: Boolean = false,
    ) = httpClientFactory.newClient(
        foreground = foreground,
        cookieKey = when (auth) {
            is BasicDigestAuthHandler -> auth.username
            is TasksBasicAuth -> auth.user
            else -> null
        },
    ) { builder ->
        builder
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
        auth?.let {
            builder.addNetworkInterceptor(it)
            if (it is Authenticator) {
                builder.authenticator(it)
            }
        }
    }
}
