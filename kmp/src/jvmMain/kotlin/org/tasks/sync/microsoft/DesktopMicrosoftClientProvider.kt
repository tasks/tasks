package org.tasks.sync.microsoft

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.tasks.auth.OAuthTokenStore
import org.tasks.auth.TasksOAuthClient
import org.tasks.auth.clientKey
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
    caldavDao: CaldavDao,
    private val okHttpClientFactory: OkHttpClientFactory,
    private val cookieDir: File,
    private val oauthClient: TasksOAuthClient = TasksOAuthClient(),
) : MicrosoftClientProvider {

    private val tokenStore = OAuthTokenStore(encryption, caldavDao, oauthClient)

    private val engineMutex = Mutex()
    @Volatile private var sharedEngine: OkHttpClient? = null

    private suspend fun sharedEngine(): OkHttpClient =
        sharedEngine ?: engineMutex.withLock {
            sharedEngine ?: okHttpClientFactory
                .newClient(foreground = false) { it.cookieJar(CookieJar.NO_COOKIES) }
                .also { sharedEngine = it }
        }

    private val clients = ConcurrentHashMap<String, ClientEntry>()

    private data class Session(val account: CaldavAccount, val token: String)

    private class ClientEntry(
        val client: HttpClient,
        val cookies: DesktopCookieStorage,
        val session: AtomicReference<Session>,
    )

    override suspend fun getService(account: CaldavAccount): MicrosoftService = withContext(Dispatchers.IO) {
        val session = Session(account, tokenStore.getToken(account))
        val engine = sharedEngine()
        val key = account.clientKey()
        val entry = clients.compute(key) { _, existing ->
            existing?.apply { this.session.set(session) }
                ?: buildClient(
                    session = session,
                    engine = engine,
                    cookies = DesktopCookieStorage.forKey(cookieDir, key, encryption),
                )
        }!!
        MicrosoftService(client = entry.client)
    }

    fun close() {
        clients.values.forEach { runCatching { it.cookies.close() } }
    }

    private fun buildClient(
        session: Session,
        engine: OkHttpClient,
        cookies: DesktopCookieStorage,
    ): ClientEntry {
        val sessionRef = AtomicReference(session)
        val client = HttpClient(OkHttp) {
            engine {
                preconfigured = engine
            }
            installMicrosoftGraph(
                cookiesStorage = cookies,
                log = { Logger.d("MicrosoftService") { it } },
                bearerToken = { sessionRef.get().token },
                refreshToken = { rejected ->
                    tokenStore.getToken(sessionRef.get().account, rejected).also { token ->
                        sessionRef.updateAndGet { it.copy(token = token) }
                    }
                },
            )
        }
        return ClientEntry(client, cookies, sessionRef)
    }

}
