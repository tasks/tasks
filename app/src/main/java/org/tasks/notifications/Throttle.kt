package org.tasks.notifications

import kotlinx.coroutines.*
import org.tasks.time.DateTimeUtils.currentTimeMillis
import timber.log.Timber
import java.util.concurrent.Executors.newSingleThreadExecutor

internal class Throttle constructor(
        ratePerPeriod: Int,
        private val periodMillis: Long = 1000,
        private val tag: String = "",
        private val scope: CoroutineScope =
                CoroutineScope(newSingleThreadExecutor().asCoroutineDispatcher() + SupervisorJob()),
        private val sleeper: suspend (Long) -> Unit = { delay(it) }) {
    private val throttle: LongArray = LongArray(ratePerPeriod)
    private var oldest = 0

    @Synchronized
    fun run(runnable: suspend () -> Unit): Job = scope.launch {
        val sleep = throttle[oldest] - (currentTimeMillis() - periodMillis)
        if (sleep > 0) {
            Timber.v("$tag: Throttled for ${sleep}ms")
            sleeper.invoke(sleep)
        }
        try {
            runnable.invoke()
        } catch (e: Exception) {
            Timber.e(e)
        }
        throttle[oldest] = currentTimeMillis()
        oldest = (oldest + 1) % throttle.size
    }
}