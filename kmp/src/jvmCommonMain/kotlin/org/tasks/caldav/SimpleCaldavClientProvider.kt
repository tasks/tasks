package org.tasks.caldav

import at.bitfire.dav4jvm.okhttp.BasicDigestAuthHandler
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences
import org.tasks.security.KeyStoreEncryption
import java.util.concurrent.TimeUnit

class SimpleCaldavClientProvider(
    private val encryption: KeyStoreEncryption,
    private val tasksPreferences: TasksPreferences,
    private val environment: TasksServerEnvironment,
) : CaldavClientProvider {

    override suspend fun forUrl(
        url: String?,
        username: String?,
        password: String?,
    ): CaldavClient {
        val tosVersion = tasksPreferences.get(TasksPreferences.acceptedTosVersion, 0)
        val auth = getAuthInterceptor(username, password, url, tosVersion)
        return CaldavClient(this, createHttpClient(auth), url?.toHttpUrlOrNull())
    }

    override suspend fun forTasksAccount(account: CaldavAccount): TasksClient {
        return forAccount(account) as TasksClient
    }

    override suspend fun forAccount(account: CaldavAccount, url: String?): CaldavClient {
        val tosVersion = tasksPreferences.get(TasksPreferences.acceptedTosVersion, 0)
        val password = encryption.decrypt(account.password) ?: ""
        val auth = getAuthInterceptor(account.username, password, account.url, tosVersion)
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
    ): Interceptor? = when {
        username.isNullOrBlank() || password.isNullOrBlank() -> null
        url?.startsWith(environment.caldavUrl) == true -> Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", Credentials.basic(username, password))
                .header("tasks-tos-version", tosVersion.toString())
                .build()
            chain.proceed(request)
        }
        else -> BasicDigestAuthHandler(null, username, password.toCharArray())
    }

    private fun createHttpClient(auth: Interceptor?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
        auth?.let {
            builder.addNetworkInterceptor(it)
            if (it is okhttp3.Authenticator) {
                builder.authenticator(it)
            }
        }
        return builder.build()
    }
}
