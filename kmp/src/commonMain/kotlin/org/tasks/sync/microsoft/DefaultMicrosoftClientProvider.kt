package org.tasks.sync.microsoft

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tasks.auth.clientKey
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.PersistentCookiesStorage
import org.tasks.http.installMicrosoftGraph
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

@OptIn(ExperimentalAtomicApi::class)
class DefaultMicrosoftClientProvider(
    private val engine: suspend () -> HttpClientEngine,
    private val cookiesStorage: (key: String) -> PersistentCookiesStorage,
    private val tokens: MicrosoftTokenSource,
    private val accountIds: suspend () -> Set<Long>,
    private val logLevel: LogLevel = LogLevel.HEADERS,
) : MicrosoftClientProvider {

    private val logger = Logger.withTag("MicrosoftClientProvider")
    private val engineMutex = Mutex()
    @Volatile private var sharedEngine: HttpClientEngine? = null
    private val mutex = Mutex()
    private val clients = HashMap<Long, ClientEntry>()

    private data class Session(val account: CaldavAccount, val token: String)

    private class ClientEntry(
        val client: HttpClient,
        val cookies: PersistentCookiesStorage,
        val session: AtomicReference<Session>,
    )

    override suspend fun hasCredentials(account: CaldavAccount): Boolean =
        tokens.hasCredentials(account)

    override suspend fun getService(account: CaldavAccount): MicrosoftService =
        withContext(Dispatchers.IO) {
            val session = Session(account, tokens.getToken(account))
            val entry = mutex.withLock { cached(session) } ?: newEntry(session)
            MicrosoftService(client = entry.client)
        }

    private fun cached(session: Session): ClientEntry? =
        clients[session.account.id]?.also { it.session.store(session) }

    private suspend fun newEntry(session: Session): ClientEntry {
        val account = session.account
        val engine = sharedEngine()
        var retired = emptyList<ClientEntry>()
        val entry = mutex.withLock {
            cached(session) ?: run {
                retired = retireRemovedAccounts()
                buildClient(engine, account.clientKey(), session)
                    .also { clients[account.id] = it }
            }
        }
        retired.forEach { removed ->
            runCatching {
                removed.cookies.discard()
                removed.client.close()
            }.onFailure { logger.e(it) { "Failed to retire a removed account" } }
        }
        return entry
    }

    private suspend fun retireRemovedAccounts(): List<ClientEntry> {
        if (clients.isEmpty()) return emptyList()
        val live = runCatching { accountIds() }
            .onFailure { logger.e(it) { "Failed to look up accounts" } }
            .getOrNull()
            ?: return emptyList()
        return (clients.keys - live).mapNotNull { clients.remove(it) }
    }

    suspend fun close() {
        val entries = mutex.withLock {
            clients.values.toList().also { clients.clear() }
        }
        entries.forEach { entry ->
            runCatching {
                entry.cookies.closeAndFlush()
                entry.client.close()
            }.onFailure { logger.e(it) { "Failed to close a client" } }
        }
        val engine = engineMutex.withLock { sharedEngine.also { sharedEngine = null } }
        runCatching { engine?.close() }
            .onFailure { logger.e(it) { "Failed to close the engine" } }
    }

    private suspend fun sharedEngine(): HttpClientEngine =
        sharedEngine ?: engineMutex.withLock {
            sharedEngine ?: engine().also { sharedEngine = it }
        }

    private fun buildClient(engine: HttpClientEngine, key: String, session: Session): ClientEntry {
        val sessionRef = AtomicReference(session)
        val cookies = cookiesStorage(key)
        val client = HttpClient(engine) {
            installMicrosoftGraph(
                cookiesStorage = cookies,
                logLevel = logLevel,
                log = { Logger.d("MicrosoftService") { it } },
                bearerToken = { sessionRef.load().token },
                refreshToken = { rejected ->
                    tokens.refreshToken(sessionRef.load().account, rejected)?.also { token ->
                        sessionRef.update { it.copy(token = token) }
                    }
                },
            )
        }
        return ClientEntry(client, cookies, sessionRef)
    }
}
