package org.tasks.notifications

import kotlinx.coroutines.delay
import org.tasks.time.DateTimeUtils

internal class Throttle constructor(
        ratePerSecond: Int,
        private val sleeper: suspend (Long) -> Unit = { delay(it) }) {
    private val throttle: LongArray = LongArray(ratePerSecond)
    private var oldest = 0

    @Synchronized
    suspend fun run(runnable: suspend () -> Unit) {
        val sleep = throttle[oldest] - (DateTimeUtils.currentTimeMillis() - 1000)
        if (sleep > 0) {
            sleeper.invoke(sleep)
        }
        runnable.invoke()
        throttle[oldest] = DateTimeUtils.currentTimeMillis()
        oldest = (oldest + 1) % throttle.size
    }
}