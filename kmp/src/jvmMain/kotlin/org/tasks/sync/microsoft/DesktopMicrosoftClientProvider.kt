package org.tasks.sync.microsoft

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.tasks.auth.OAuthTokenData
import org.tasks.auth.OAuthTokenRefresh
import org.tasks.auth.OAuthTokenRefresh.withRefreshResult
import org.tasks.auth.TasksOAuthClient
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.DesktopCookieStorage
import org.tasks.http.OkHttpClientFactory
import org.tasks.http.installMicrosoftGraph
import org.tasks.security.KeyStoreEncryption
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class DesktopMicrosoftClientProvider(
    private val encryption: KeyStoreEncryption,
    private val caldavDao: CaldavDao,
    private val okHttpClientFactory: OkHttpClientFactory,
    private val cookieDir: File,
    private val oauthClient: TasksOAuthClient = TasksOAuthClient(),
) : MicrosoftClientProvider {

    private val locks = ConcurrentHashMap<String, Mutex>()

    private val engineMutex = Mutex()
    @Volatile private var sharedEngine: OkHttpClient? = null

    private suspend fun sharedEngine(): OkHttpClient =
        sharedEngine ?: engineMutex.withLock {
            sharedEngine ?: okHttpClientFactory
                .newClient(foreground = false) { it.cookieJar(CookieJar.NO_COOKIES) }
                .also { sharedEngine = it }
        }

    private val clients = ConcurrentHashMap<String, ClientEntry>()

    private val tokenCache = ConcurrentHashMap<Long, CachedToken>()

    private class ClientEntry(
        val client: HttpClient,
        val cookies: DesktopCookieStorage,
        val token: AtomicReference<String>,
    )
    private class CachedToken(val encrypted: String, val data: OAuthTokenData)

    override suspend fun getService(account: CaldavAccount): MicrosoftService = withContext(Dispatchers.IO) {
        val token = getToken(account)
        val engine = sharedEngine()
        val key = account.clientKey()
        val entry = clients.computeIfAbsent(key) {
            buildClient(
                initialToken = token,
                engine = engine,
                cookies = DesktopCookieStorage.forKey(cookieDir, key, encryption),
                refresh = { getToken(account) },
            )
        }
        entry.token.set(token)
        MicrosoftService(client = entry.client)
    }

    fun close() {
        clients.values.forEach { runCatching { it.cookies.close() } }
    }

    private fun buildClient(
        initialToken: String,
        engine: OkHttpClient,
        cookies: DesktopCookieStorage,
        refresh: suspend () -> String,
    ): ClientEntry {
        val tokenRef = AtomicReference(initialToken)
        val client = HttpClient(OkHttp) {
            engine {
                preconfigured = engine
            }
            installMicrosoftGraph(
                cookiesStorage = cookies,
                log = { Logger.d("MicrosoftService") { it } },
                bearerToken = { tokenRef.get() },
            )
        }
        client.plugin(HttpSend).intercept { request ->
            val call = execute(request)
            if (call.response.status != HttpStatusCode.Unauthorized) {
                return@intercept call
            }
            val refreshed = refresh()
            if (call.request.headers[HttpHeaders.Authorization] == "Bearer $refreshed") {
                return@intercept call
            }
            tokenRef.set(refreshed)
            request.headers[HttpHeaders.Authorization] = "Bearer $refreshed"
            execute(request)
        }
        return ClientEntry(client, cookies, tokenRef)
    }

    private suspend fun getToken(account: CaldavAccount): String {
        return locks.computeIfAbsent(account.clientKey()) { Mutex() }.withLock {
            val id = account.id.takeIf { it != 0L }
            val encrypted = (id?.let { caldavDao.getAccount(it) }?.password)
                ?: account.password
                ?: throw IllegalStateException("Missing credentials")
            val data = id?.let { tokenCache[it] }?.takeIf { it.encrypted == encrypted }?.data
                ?: run {
                    val decrypted = encryption.decrypt(encrypted)
                        ?: throw IllegalStateException("Failed to decrypt credentials")
                    OAuthTokenData.deserialize(decrypted).also { decoded ->
                        id?.let { tokenCache[it] = CachedToken(encrypted, decoded) }
                    }
                }
            if (!OAuthTokenRefresh.isExpired(data, refreshWhenExpiryUnknown = true)) {
                account.password = encrypted
                return@withLock data.accessToken
            }
            Logger.d(TAG) { "Token expired, refreshing" }
            return@withLock withContext(NonCancellable) {
                val result = OAuthTokenRefresh.refreshOrThrowIO {
                    oauthClient.refreshToken(
                        tokenEndpoint = data.tokenEndpoint,
                        clientId = data.clientId,
                        refreshToken = data.refreshToken,
                    )
                }
                val updated = data.withRefreshResult(result)
                val newEncrypted = encryption.encrypt(updated.serialize())
                    ?: throw IllegalStateException("Failed to encrypt credentials")
                account.password = newEncrypted
                id?.let {
                    caldavDao.setPassword(it, newEncrypted)
                    tokenCache[it] = CachedToken(newEncrypted, updated)
                } ?: Logger.w(TAG) {
                    "Refreshed token for a detached account (id=0) cannot be persisted; the next sync " +
                        "may replay the now-spent refresh token and log the account out"
                }
                result.accessToken
            }
        }
    }

    private fun CaldavAccount.clientKey(): String =
        username?.takeIf { it.isNotBlank() } ?: uuid ?: ""

    companion object {
        private const val TAG = "MicrosoftClientProvider"
    }
}
