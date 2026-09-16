package org.tasks.fcm

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import org.tasks.ensureStarted
import org.tasks.preferences.TasksPreferences
import org.tasks.sync.SyncRunner
import org.tasks.sync.SyncSource

private const val TAG = "ApnsPush"

private const val PUSH_SYNC_TIMEOUT_MS = 25_000L

class ApnsTokenProvider(
    private val tasksPreferences: TasksPreferences,
    private val scope: CoroutineScope,
    private val pushTokenManager: () -> PushTokenManager,
) : FcmTokenProvider {
    private val token = MutableStateFlow<String?>(null)

    override val provider = FcmTokenProvider.PROVIDER_APNS

    override suspend fun getToken(): String? = token.value ?: tasksPreferences.get(TasksPreferences.apnsToken, "").ifBlank { null }

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
