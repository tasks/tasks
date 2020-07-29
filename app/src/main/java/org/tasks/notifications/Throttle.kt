package org.tasks.notifications

import kotlinx.coroutines.delay
import org.tasks.time.DateTimeUtils.currentTimeMillis
import timber.log.Timber

internal class Throttle constructor(
        ratePerPeriod: Int,
        private val periodMillis: Long = 1000,
        private val tag: String = "",
        private val sleeper: suspend (Long) -> Unit = { delay(it) }) {
    private val throttle: LongArray = LongArray(ratePerPeriod)
    private var oldest = 0

    @Synchronized
    suspend fun run(runnable: suspend () -> Unit) {
        val sleep = throttle[oldest] - (currentTimeMillis() - periodMillis)
        if (sleep > 0) {
            Timber.d("$tag: Throttled for ${sleep}ms")
            sleeper.invoke(sleep)
        }
        runnable.invoke()
        throttle[oldest] = currentTimeMillis()
        oldest = (oldest + 1) % throttle.size
    }
}