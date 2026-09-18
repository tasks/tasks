package org.tasks.sse

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.basicAuth
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.KtorClientFactory
import org.tasks.jobs.BackgroundWork
import org.tasks.security.Encryption
import org.tasks.sync.SyncSource
import kotlin.coroutines.cancellation.CancellationException

class SseClient(
    private val scope: CoroutineScope,
    private val backgroundWork: BackgroundWork,
    private val caldavDao: CaldavDao,
    private val encryption: Encryption,
    private val environment: TasksServerEnvironment,
    private val httpClientFactory: KtorClientFactory,
    private val token: suspend () -> String?,
) {
    private val mutex = Mutex()
    private var watchJob: Job? = null
    private var connectionJob: Job? = null
    private var backoffMs = INITIAL_BACKOFF_MS

    fun start() {
        scope.launch {
            mutex.withLock {
                if (watchJob?.isActive == true) return@withLock
                watchJob = scope.launch {
                    caldavDao.watchAccounts()
                        .map { accounts -> accounts.any { it.accountType == CaldavAccount.TYPE_TASKS } }
                        .distinctUntilChanged()
                        .collect { hasTasksAccount ->
                            mutex.withLock {
                                if (hasTasksAccount) {
                                    startConnectionLocked()
                                } else {
                                    stopConnectionLocked()
                                    Logger.i(TAG) { "SSE stopped: no Tasks.org account" }
                                }
                            }
                        }
                }
            }
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                watchJob?.cancelAndJoin()
                watchJob = null
                stopConnectionLocked()
            }
        }
    }

    fun reconnect() {
        scope.launch {
            mutex.withLock {
                if (watchJob?.isActive != true) return@withLock
                Logger.i(TAG) { "SSE reconnect requested" }
                stopConnectionLocked()
                startConnectionLocked()
            }
        }
    }

    private fun startConnectionLocked() {
        if (connectionJob?.isActive == true) return
        backoffMs = INITIAL_BACKOFF_MS
        connectionJob = scope.launch(Dispatchers.Default) {
            while (true) {
                val account = caldavDao.getAccounts(CaldavAccount.TYPE_TASKS).firstOrNull()
                    ?: break
                try {
                    connect(account)
                    backoffMs = INITIAL_BACKOFF_MS
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(TAG) { "SSE connection failed: ${e.message}" }
                }
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    private suspend fun stopConnectionLocked() {
        connectionJob?.cancelAndJoin()
        connectionJob = null
    }

    private suspend fun connect(account: CaldavAccount) {
        val password = encryption.decrypt(account.password) ?: return
        val token = token() ?: return
        val baseUrl = environment.caldavUrl.trimEnd('/')
        val client = httpClientFactory.newClient(foreground = false) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = READ_TIMEOUT_MS
            }
        }
        try {
            Logger.d(TAG) { "SSE connecting to $baseUrl" }
            client
                .prepareGet("$baseUrl/sse?token=$token") {
                    header(HttpHeaders.Accept, "text/event-stream")
                    basicAuth(account.username ?: "", password)
                }
                .execute { response ->
                    if (!response.status.isSuccess()) {
                        Logger.w(TAG) { "SSE connection rejected: ${response.status.value}" }
                        return@execute
                    }
                    Logger.i(TAG) { "SSE connected" }
                    val channel = response.bodyAsChannel()
                    while (true) {
                        val line = withTimeoutOrNull(READ_TIMEOUT_MS) { channel.readUTF8Line() } ?: break
                        onLine(line)
                    }
                }
        } finally {
            client.close()
        }
    }

    private fun onLine(line: String) {
        if (line.startsWith("data:") && line.removePrefix("data:").contains("\"sync\"")) {
            Logger.d(TAG) { "SSE sync event received" }
            scope.launch { backgroundWork.sync(SyncSource.PUSH_NOTIFICATION) }
        }
    }

    companion object {
        private const val TAG = "SseClient"
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val CONNECT_TIMEOUT_MS = 30_000L
        private const val READ_TIMEOUT_MS = 90_000L
    }
}
