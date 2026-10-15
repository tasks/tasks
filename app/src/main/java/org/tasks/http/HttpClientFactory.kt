package org.tasks.http

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.data.entity.CaldavAccount
import org.tasks.extensions.Context.cookiePersistor
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.microsoft.MicrosoftClientProvider
import org.tasks.sync.microsoft.MicrosoftService
import org.tasks.sync.microsoft.MicrosoftTokenProvider
import org.tasks.sync.todoist.TodoistClientProvider
import org.tasks.sync.todoist.TodoistService
import org.tasks.sync.todoist.TodoistTokenProvider
import timber.log.Timber
import javax.inject.Inject

class HttpClientFactory @Inject constructor(
    @ApplicationContext context: Context,
    private val encryption: KeyStoreEncryption,
    private val microsoftTokenProvider: MicrosoftTokenProvider,
    private val todoistTokenProvider: TodoistTokenProvider,
) : AndroidOkHttpClientFactory(
    context = context,
    userAgent = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}) Android/${android.os.Build.VERSION.RELEASE}",
), MicrosoftClientProvider {

    override suspend fun getService(account: CaldavAccount): MicrosoftService =
        getMicrosoftService(account)

    override suspend fun hasCredentials(account: CaldavAccount): Boolean =
        microsoftTokenProvider.hasCredentials(account)

    /**
     * Separate from [MicrosoftClientProvider] because both interfaces declare
     * `getService(CaldavAccount)` with different return types, which a single class
     * cannot implement twice.
     */
    val todoist: TodoistClientProvider = object : TodoistClientProvider {
        override suspend fun getService(account: CaldavAccount): TodoistService =
            newTodoistService(todoistTokenProvider.getToken(account))

        override suspend fun getService(token: String): TodoistService =
            newTodoistService(token)

        override suspend fun hasCredentials(account: CaldavAccount): Boolean =
            todoistTokenProvider.hasCredentials(account)
    }

    private suspend fun newTodoistService(token: String): TodoistService =
        withContext(Dispatchers.IO) {
            TodoistService(
                HttpClient(Android) {
                    installTodoist(
                        logLevel = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.HEADERS,
                        log = { Timber.d(it) },
                        bearerToken = { token },
                    )
                }
            )
        }

    suspend fun newClient(foreground: Boolean) = newClient(
        foreground = foreground,
        cookieKey = null,
        block = {},
    )

    suspend fun newAuthenticatedClient(
        foreground: Boolean = false,
        username: String? = null,
        encryptedPassword: String? = null,
        url: String? = null,
    ): HttpClient {
        val decrypted = encryptedPassword?.let { encryption.decrypt(it) }
        return newClient(foreground = foreground, cookieKey = username).toKtor {
            if (!username.isNullOrBlank() && !decrypted.isNullOrBlank()) {
                installBasicDigestAuth(username, decrypted, url)
            }
        }
    }

    suspend fun getMicrosoftService(account: CaldavAccount): MicrosoftService = withContext(Dispatchers.IO) {
        val token = microsoftTokenProvider.getToken(account)
        val client = HttpClient(Android) {
            installMicrosoftGraph(
                cookiesStorage = AndroidCookieStorage(context = context, key = account.username),
                logLevel = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.HEADERS,
                log = { Timber.d(it) },
                bearerToken = { token },
            )
        }
        MicrosoftService(
            client = client
        )
    }
}
