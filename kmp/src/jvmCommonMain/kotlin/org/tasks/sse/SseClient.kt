package org.tasks.sse

import co.touchlab.kermit.Logger
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
import okhttp3.Request
import okhttp3.Response
import org.tasks.auth.TasksServerEnvironment
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.OkHttpClientFactory
import org.tasks.jobs.BackgroundWork
import org.tasks.security.KeyStoreEncryption
import org.tasks.sync.SyncSource
import java.io.IOException
import java.util.concurrent.TimeUnit

class SseClient(
    private val scope: CoroutineScope,
    private val backgroundWork: BackgroundWork,
    private val caldavDao: CaldavDao,
    private val encryption: KeyStoreEncryption,
    private val environment: TasksServerEnvironment,
    private val httpClientFactory: OkHttpClientFactory,
    private val tokenProvider: SseTokenProvider,
) {
    private val mutex = Mutex()
    private var connectionJob: Job? = null
    @Volatile private var currentCall: okhttp3.Call? = null
    private var backoffMs = INITIAL_BACKOFF_MS

    fun start() {
        scope.launch {
            caldavDao.watchAccounts()
                .map { accounts -> accounts.any { it.accountType == CaldavAccount.TYPE_TASKS } }
                .distinctUntilChanged()
                .collect { hasTasksAccount ->
                    if (hasTasksAccount) {
                        mutex.withLock { startConnectionLocked() }
                    } else {
                        mutex.withLock {
                            stopConnectionLocked()
                            Logger.i(TAG) { "SSE stopped: no Tasks.org account" }
                        }
                    }
                }
        }
    }

    private fun startConnectionLocked() {
        if (connectionJob?.isActive == true) return
        backoffMs = INITIAL_BACKOFF_MS
        connectionJob = scope.launch(Dispatchers.IO) {
            while (true) {
                val account = caldavDao.getAccounts(CaldavAccount.TYPE_TASKS).firstOrNull()
                    ?: break
                try {
                    connect(account)
                    backoffMs = INITIAL_BACKOFF_MS
                } catch (e: IOException) {
                    Logger.w(TAG) { "SSE connection failed: ${e.message}" }
                } catch (e: Exception) {
                    Logger.e(TAG, e) { "SSE unexpected error" }
                }
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    private suspend fun stopConnectionLocked() {
        currentCall?.cancel()
        currentCall = null
        connectionJob?.cancelAndJoin()
        connectionJob = null
    }

    fun reconnect() {
        scope.launch {
            mutex.withLock {
                Logger.i(TAG) { "SSE reconnect requested" }
                stopConnectionLocked()
                startConnectionLocked()
            }
        }
    }

    private suspend fun connect(account: CaldavAccount) {
        val password = encryption.decrypt(account.password) ?: return
        val baseUrl = environment.caldavUrl.trimEnd('/')
        val url = "$baseUrl/sse?token=${tokenProvider.token}"

        val client = httpClientFactory.newClient(foreground = false) { builder ->
            builder
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
            builder.addNetworkInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", okhttp3.Credentials.basic(
                            account.username ?: "",
                            password,
                            Charsets.UTF_8,
                        ))
                        .build()
                )
            }
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .build()

        Logger.d(TAG) { "SSE connecting to $baseUrl" }
        val call = client.newCall(request)
        currentCall = call
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.w(TAG) { "SSE connection rejected: ${response.code}" }
                    return
                }
                Logger.i(TAG) { "SSE connected" }
                readStream(response)
            }
        } finally {
            currentCall = null
        }
    }

    private fun readStream(response: Response) {
        val reader = response.body?.source()?.inputStream()?.bufferedReader()
            ?: return
        reader.use {
            var line = it.readLine()
            while (line != null) {
                when {
                    line.startsWith("data:") -> {
                        val data = line.removePrefix("data:").trim()
                        if (data.contains("\"sync\"")) {
                            Logger.d(TAG) { "SSE sync event received" }
                            scope.launch {
                                backgroundWork.sync(SyncSource.PUSH_NOTIFICATION)
                            }
                        }
                    }
                    line.startsWith(":") -> {
                        // Comment (heartbeat), ignore
                    }
                }
                line = it.readLine()
            }
        }
    }

    companion object {
        private const val TAG = "SseClient"
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val READ_TIMEOUT_SECONDS = 90L
    }
}
