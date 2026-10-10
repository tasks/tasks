package org.tasks.sync.todoist

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.OkHttpClientFactory
import org.tasks.http.installTodoist
import org.tasks.security.KeyStoreEncryption

/**
 * Desktop [TodoistClientProvider].
 *
 * Much simpler than [org.tasks.sync.microsoft.DesktopMicrosoftClientProvider]: a
 * Todoist personal API token doesn't expire, so there is no refresh, no token cache
 * and no 401-retry interceptor. The OkHttp engine is shared across clients; cookies
 * are disabled because the API is a pure bearer-token API.
 */
class DesktopTodoistClientProvider(
    private val encryption: KeyStoreEncryption,
    private val okHttpClientFactory: OkHttpClientFactory,
) : TodoistClientProvider {

    private val tokenProvider = TodoistTokenProvider(encryption)

    private val engineMutex = Mutex()
    @Volatile private var sharedEngine: OkHttpClient? = null

    private suspend fun sharedEngine(): OkHttpClient =
        sharedEngine ?: engineMutex.withLock {
            sharedEngine ?: okHttpClientFactory
                .newClient(foreground = false) { it.cookieJar(CookieJar.NO_COOKIES) }
                .also { sharedEngine = it }
        }

    override suspend fun getService(account: CaldavAccount): TodoistService =
        getService(tokenProvider.getToken(account))

    override suspend fun getService(token: String): TodoistService =
        withContext(Dispatchers.IO) {
            val engine = sharedEngine()
            TodoistService(
                HttpClient(OkHttp) {
                    engine { preconfigured = engine }
                    installTodoist(
                        log = { Logger.d(TAG) { it } },
                        bearerToken = { token },
                    )
                }
            )
        }

    override suspend fun hasCredentials(account: CaldavAccount): Boolean =
        tokenProvider.hasCredentials(account)

    companion object {
        private const val TAG = "TodoistService"
    }
}
