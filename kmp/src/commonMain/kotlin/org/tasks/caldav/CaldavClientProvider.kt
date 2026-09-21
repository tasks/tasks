package org.tasks.caldav

import at.bitfire.dav4jvm.ktor.PreemptiveBasicDigestAuthProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.entity.CaldavAccount
import org.tasks.fcm.FcmTokenProvider
import org.tasks.http.KtorClientFactory
import org.tasks.preferences.TasksPreferences
import org.tasks.security.Encryption

class CaldavClientProvider(
    private val encryption: Encryption,
    private val tasksPreferences: TasksPreferences,
    private val environment: TasksServerEnvironment,
    private val httpClientFactory: KtorClientFactory,
    private val tokenProvider: FcmTokenProvider? = null,
    private val subscriptionProvider: () -> TasksBasicAuth.SubscriptionInfo? = { null },
) : CaldavClientFactory {

    private sealed interface CaldavAuth {
        val user: String

        class Tasks(val plugin: TasksBasicAuth) : CaldavAuth {
            override val user get() = plugin.user
        }

        class BasicDigest(override val user: String, val password: String) : CaldavAuth
    }

    override suspend fun forUrl(
        url: String?,
        username: String?,
        password: String?,
    ): CaldavClient {
        val tosVersion = tasksPreferences.get(TasksPreferences.acceptedTosVersion, 0)
        val httpUrl = url?.toCaldavUrl()
        val auth = getAuth(username, password, url, tosVersion)
        return CaldavClient(createHttpClient(auth = auth, foreground = true), httpUrl)
    }

    override suspend fun forTasksAccount(account: CaldavAccount): TasksClient {
        if (!account.isTasksOrg) {
            throw IllegalArgumentException()
        }
        return forAccount(account) as TasksClient
    }

    override suspend fun forAccount(account: CaldavAccount, url: String?): CaldavClient {
        val httpUrl = url?.toCaldavUrl()
        val tosVersion = tasksPreferences.get(TasksPreferences.acceptedTosVersion, 0)
        val password = encryption.decrypt(account.password) ?: ""
        val pushToken = if (account.isTasksOrg) tokenProvider?.getToken() else null
        val auth = getAuth(
            account.username,
            password,
            account.url,
            tosVersion,
            pushToken,
        )
        val client = createHttpClient(auth)
        return if (account.isTasksOrg) {
            TasksClient(client, httpUrl)
        } else {
            CaldavClient(client, httpUrl)
        }
    }

    private fun getAuth(
        username: String?,
        password: String?,
        url: String?,
        tosVersion: Int,
        pushToken: String? = null,
    ): CaldavAuth? = when {
        username.isNullOrBlank() || password.isNullOrBlank() -> null
        url?.startsWith(environment.caldavUrl) == true -> CaldavAuth.Tasks(
            TasksBasicAuth(
                user = username,
                token = password,
                tosVersion = tosVersion,
                pushToken = pushToken,
                subscriptionInfo = subscriptionProvider(),
            )
        )
        else -> CaldavAuth.BasicDigest(username, password)
    }

    private suspend fun createHttpClient(
        auth: CaldavAuth?,
        foreground: Boolean = false,
    ): HttpClient = httpClientFactory.newClient(foreground = foreground, cookieKey = auth?.user) {
        followRedirects = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 120_000
        }
        when (auth) {
            is CaldavAuth.Tasks -> install(auth.plugin)
            is CaldavAuth.BasicDigest -> install(Auth) {
                providers.add(PreemptiveBasicDigestAuthProvider(auth.user, auth.password))
            }
            null -> {}
        }
    }
}
