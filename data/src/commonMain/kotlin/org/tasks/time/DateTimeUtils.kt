package org.tasks.time

import org.tasks.IS_DEBUG
import org.tasks.printTimestamp

object DateTimeUtils2 {
    fun currentTimeMillis(): Long {
        return MILLIS_PROVIDER.millis
    }

    private val SYSTEM_MILLIS_PROVIDER = SystemMillisProvider()

    @kotlin.concurrent.Volatile
    private var MILLIS_PROVIDER: MillisProvider = SYSTEM_MILLIS_PROVIDER

    fun setCurrentMillisFixed(millis: Long) {
        MILLIS_PROVIDER = FixedMillisProvider(millis)
    }

    fun setCurrentMillisSystem() {
        MILLIS_PROVIDER = SYSTEM_MILLIS_PROVIDER
    }
}

fun printTimestamp(timestamp: Long): String =
    if (IS_DEBUG) timestamp.printTimestamp() else timestamp.toString()
