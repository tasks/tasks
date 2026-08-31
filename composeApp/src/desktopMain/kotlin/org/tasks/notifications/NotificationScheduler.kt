package org.tasks.notifications

import co.touchlab.kermit.Logger
import com.todoroo.astrid.alarms.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.tasks.extensions.guarded
import org.tasks.data.entity.Notification
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

private const val TAG = "NotificationScheduler"

enum class Hold {
    NONE,

    UNTIL_SIGNALLED,

    UNTIL_NEXT_ALARM,
}

class NotificationScheduler(
    private val alarmService: () -> AlarmService,
    private val trigger: suspend (List<Notification>) -> Collection<Long>,
    private val hold: suspend () -> Hold = { Hold.NONE },
) {
    private val wakeUps = Channel<Unit>(Channel.CONFLATED)

    private val lock = Any()

    private var job: Job? = null

    private var reconcileJob: Job? = null

    private var unwinding: List<Job> = emptyList()

    fun start(
        scope: CoroutineScope,
        context: CoroutineContext = EmptyCoroutineContext,
        reconcile: suspend () -> Unit = {},
    ) {
        synchronized(lock) {
            if (job?.isActive == true) {
                return
            }

            unwinding = (unwinding + listOfNotNull(job, reconcileJob)).filterNot { it.isCompleted }

            val previous = reconcileJob
            if (previous?.isActive != true) {
                reconcileJob = scope.launch(context) {
                    previous?.join()
                    reconcile()
                }
            }
            job = scope.launch(context) { run() }
        }
    }

    suspend fun stop() {
        val running = synchronized(lock) {
            (unwinding + listOfNotNull(job, reconcileJob)).distinct().also {
                this.job = null
                this.reconcileJob = null
                this.unwinding = emptyList()
            }
        }
        if (running.isEmpty()) {
            return
        }
        running.forEach { it.cancel() }

        val stopped = withTimeoutOrNull(STOP_TIMEOUT_MS) { running.joinAll() }
        if (stopped == null) {
            synchronized(lock) {
                unwinding = (unwinding + running.filterNot { it.isCompleted }).distinct()
            }
            Logger.w(tag = TAG) { "Timed out waiting for the scan to stop" }
        }
    }

    fun signal() {
        wakeUps.trySend(Unit)
    }

    internal suspend fun run() {
        while (coroutineContext.isActive) {
            val wait = scanOnce().coerceAtMost(MAX_HOLD)

            delay(MIN_WAIT)
            withTimeoutOrNull(wait - MIN_WAIT) { wakeUps.receive() }
        }
    }

    internal suspend fun scanOnce(): Long =

        guarded(TAG, "Failed to trigger alarms", MAX_WAIT) {
            when (hold()) {
                Hold.NONE -> scan()
                Hold.UNTIL_SIGNALLED -> {
                    Logger.d(tag = TAG) { "Nothing to post until reminders are switched back on" }
                    HELD
                }
                Hold.UNTIL_NEXT_ALARM -> waitForTheNextAlarm()
            }
        }

    private suspend fun waitForTheNextAlarm(): Long {
        val wait = holdUntil(alarmService().triggerAlarms { emptyList() })
        Logger.d(tag = TAG) {
            if (wait == HELD) {
                "Nothing to post and nothing else due, waiting to be signalled"
            } else {
                "Nothing to post, waiting ${wait}ms for the next alarm"
            }
        }
        return wait
    }

    private suspend fun scan(): Long {
        val triggered = mutableListOf<Long>()
        val wait = waitFor(
            alarmService().triggerAlarms { entries ->
                trigger(entries).also { triggered.addAll(it) }
            }
        )
        if (triggered.isNotEmpty()) {
            Logger.d(tag = TAG) { "Scan fired $triggered, next in ${wait}ms" }
        }
        return wait
    }

    companion object {
        internal const val MAX_WAIT = 60_000L

        internal const val MAX_HOLD = 15 * MAX_WAIT

        internal const val HELD = Long.MAX_VALUE

        internal const val MIN_WAIT = 1_000L

        private const val STOP_TIMEOUT_MS = 2_000L

        internal fun waitFor(nextAlarm: Long, now: Long = currentTimeMillis()): Long =
            if (nextAlarm > 0) {
                (nextAlarm - now).coerceIn(MIN_WAIT, MAX_WAIT)
            } else {
                MAX_WAIT
            }

        internal fun holdUntil(nextAlarm: Long, now: Long = currentTimeMillis()): Long =
            if (nextAlarm > 0) (nextAlarm - now).coerceIn(MIN_WAIT, MAX_HOLD) else HELD
    }
}
