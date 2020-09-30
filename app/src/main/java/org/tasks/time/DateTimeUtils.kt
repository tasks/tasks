package org.tasks.time

import android.annotation.SuppressLint
import org.tasks.BuildConfig
import org.tasks.date.DateTimeUtils.newDateTime
import java.util.*

object DateTimeUtils {
    private val SYSTEM_MILLIS_PROVIDER = SystemMillisProvider()

    @Volatile
    private var MILLIS_PROVIDER: MillisProvider = SYSTEM_MILLIS_PROVIDER

    @JvmStatic
    fun currentTimeMillis(): Long {
        return MILLIS_PROVIDER.millis
    }

    fun setCurrentMillisFixed(millis: Long) {
        MILLIS_PROVIDER = FixedMillisProvider(millis)
    }

    fun setCurrentMillisSystem() {
        MILLIS_PROVIDER = SYSTEM_MILLIS_PROVIDER
    }

    fun printTimestamp(timestamp: Long): String {
        return if (BuildConfig.DEBUG) Date(timestamp).toString() else timestamp.toString()
    }

    @SuppressLint("DefaultLocale")
    fun printDuration(millis: Long): String {
        return if (BuildConfig.DEBUG) {
            val seconds = millis / 1000
            String.format(
                    "%dh %dm %ds", seconds / 3600L, (seconds % 3600L / 60L).toInt(), (seconds % 60L).toInt())
        } else {
            millis.toString()
        }
    }

    fun Long.startOfDay(): Long = newDateTime(this).startOfDay().millis
}