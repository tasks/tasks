package org.tasks.fcm

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import org.tasks.data.UUIDHelper
import org.tasks.ensureStarted
import org.tasks.preferences.TasksPreferences
import org.tasks.sse.SseClient
import org.tasks.sync.SyncRunner
import org.tasks.sync.SyncSource

private const val TAG = "ApnsPush"

private const val PUSH_SYNC_TIMEOUT_MS = 25_000L

class ApnsTokenProvider(
    private val tasksPreferences: TasksPreferences,
    private val scope: CoroutineScope,
    private val pushTokenManager: () -> PushTokenManager,
    private val sseClient: () -> SseClient,
) : FcmTokenProvider {
    private val token = MutableStateFlow<String?>(null)

    override val provider: String?
        get() = if (token.value != null) FcmTokenProvider.PROVIDER_APNS else null

    override suspend fun getToken(): String =
        token.value ?: storedApnsToken()?.also { token.value = it } ?: fallbackToken()

    private suspend fun storedApnsToken(): String? = tasksPreferences.get(TasksPreferences.apnsToken, "").ifBlank { null }

    private suspend fun fallbackToken(): String {
        val stored = tasksPreferences.get(TasksPreferences.pushFallbackToken, "")
        if (stored.isNotBlank()) return stored
        return UUIDHelper.newUUID().also { tasksPreferences.set(TasksPreferences.pushFallbackToken, it) }
    }

    fun onToken(value: String) {
        val previous = token.value
        token.value = value
        scope.launch {
            val stored = tasksPreferences.get(TasksPreferences.apnsToken, "")
            if (stored != value) {
                tasksPreferences.set(TasksPreferences.apnsToken, value)
            }
            if (previous != value) {
                Logger.d(TAG) { "APNs token ${if (stored == value) "unchanged" else "changed"}, registering" }
                pushTokenManager().registerTokenForAllAccounts()
                if (stored != value) {
                    sseClient().reconnect()
                }
            }
        }
    }
}

fun onApnsToken(token: String) {
    ensureStarted()
    KoinPlatform.getKoin().get<ApnsTokenProvider>().onToken(token)
}

fun onApnsPush(completion: (Boolean) -> Unit) {
    ensureStarted()
    Logger.i(TAG) { "push received" }
    val koin = KoinPlatform.getKoin()
    koin.get<CoroutineScope>().launch {
        val finished = withTimeoutOrNull(PUSH_SYNC_TIMEOUT_MS) {
            koin.get<SyncRunner>().syncNow(SyncSource.PUSH_NOTIFICATION)
            true
        } ?: false
        Logger.d(TAG) { "push sync ${if (finished) "finished" else "timed out"}" }
        withContext(Dispatchers.Main) { completion(finished) }
    }
}
