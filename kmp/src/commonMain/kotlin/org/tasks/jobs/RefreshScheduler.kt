package org.tasks.jobs

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.extensions.guarded
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.plusDays
import org.tasks.time.printTimestamp
import org.tasks.time.startOfDay
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

private const val TAG = "RefreshScheduler"

class RefreshScheduler(
    private val nextRefresh: suspend (after: Long) -> Long,
    private val refresh: () -> Unit,
) {
    private val wakeUps = Channel<Unit>(Channel.CONFLATED)

    private val requested = MutableStateFlow(NEVER)

    @Volatile
    private var job: Job? = null

    fun start(scope: CoroutineScope, context: CoroutineContext = EmptyCoroutineContext) {
        if (job?.isActive == true) {
            return
        }
        job = scope.launch(context) { run() }
    }

    fun schedule(timestamp: Long) {
        requested.update { minOf(it, timestamp) }
        signal()
    }

    fun signal() {
        wakeUps.trySend(Unit)
    }

    internal suspend fun run() {
        var since = currentTimeMillis()
        while (coroutineContext.isActive) {
            val now = currentTimeMillis()
            val wait = waitFor(refreshIfDue(since, now))
            since = now
            delay(MIN_WAIT)
            withTimeoutOrNull(wait - MIN_WAIT) { wakeUps.receive() }
        }
    }

    private suspend fun refreshIfDue(since: Long, now: Long): Long {
        val due = nextDue(since)
        if (due > now) {
            return due
        }
        requested.update { if (it <= now) NEVER else it }
        Logger.d(tag = TAG) { "Refreshing, ${printTimestamp(due)} has passed" }
        refresh()
        return nextDue(now)
    }

    private suspend fun nextDue(after: Long): Long = minOf(
        guarded(TAG, "Failed to find the next refresh", NEVER) { nextRefresh(after) },
        midnight(after),
        requested.value,
    )

    companion object {
        internal const val NEVER = Long.MAX_VALUE

        internal const val MIN_WAIT = 1_000L

        internal const val MAX_WAIT = 60_000L

        internal fun midnight(after: Long): Long = after.startOfDay().plusDays(1)

        internal fun waitFor(due: Long, now: Long = currentTimeMillis()): Long =
            (due - now).coerceIn(MIN_WAIT, MAX_WAIT)
    }
}
