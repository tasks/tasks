package org.tasks.caldav

import android.content.Context
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.getPassword
import org.tasks.http.HttpClientFactory
import org.tasks.security.KeyStoreEncryption
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CaldavClientProvider @Inject constructor(
        @ApplicationContext private val context: Context,
        private val encryption: KeyStoreEncryption,
        private val inventory: Inventory,
        private val httpClientFactory: HttpClientFactory,
) {
    private val tasksUrl = context.getString(R.string.tasks_caldav_url)

    suspend fun forUrl(
            url: String?,
            username: String? = null,
            password: String? = null
    ): CaldavClient {
        val auth = getAuthInterceptor(username, password, url)
        return CaldavClient(
                this,
                createHttpClient(
                    auth = auth,
                    foreground = true
                ),
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
            url: String?
    ): Interceptor? = when {
        username.isNullOrBlank() || password.isNullOrBlank() -> null
        url?.startsWith(tasksUrl) == true -> TasksBasicAuth(username, password, inventory)
        else -> BasicDigestAuthHandler(null, username, password)
    }

    private suspend fun createHttpClient(
        auth: Interceptor?,
        foreground: Boolean = false,
    ): OkHttpClient {
        return httpClientFactory.newClient(
            foreground = foreground,
            cookieKey = when (auth) {
                is BasicDigestAuthHandler -> auth.username
                is TasksBasicAuth -> auth.user
                else -> null
            }
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
}