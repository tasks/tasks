package org.tasks.notifications

import kotlinx.coroutines.runBlocking
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors.newSingleThreadExecutor

internal class Throttle(
        ratePerPeriod: Int,
        private val periodMillis: Long = 1000,
        private val tag: String = "",
        private val executor: Executor = newSingleThreadExecutor(),
        private val sleeper: (Long) -> Unit = { Thread.sleep(it) }) {
    private val throttle: LongArray = LongArray(ratePerPeriod)
    private var oldest = 0

    @Synchronized
    fun run(runnable: suspend () -> Unit) {
        executor.execute {
            val sleep = throttle[oldest] - (currentTimeMillis() - periodMillis)
            if (sleep > 0) {
                Timber.v("$tag: Throttled for ${sleep}ms")
                sleeper(sleep)
            }
            try {
                runBlocking {
                    runnable()
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            throttle[oldest] = currentTimeMillis()
            oldest = (oldest + 1) % throttle.size
        }
    }

    fun pause(millis: Long) = executor.execute {
        Thread.sleep(millis)
    }
}
