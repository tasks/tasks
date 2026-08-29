package org.tasks

import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.DateTimeUtils2.setCurrentMillisFixed
import org.tasks.time.DateTimeUtils2.setCurrentMillisSystem

class SuspendFreeze {

    suspend fun <T> thawAfter(run: suspend () -> T): T {
        try {
            return run()
        } finally {
            thaw()
        }
    }

    companion object {

        suspend fun <T> freezeClock(run: suspend () -> T): T {
            return freezeAt(currentTimeMillis()).thawAfter(run)
        }

        suspend fun <T> freezeAt(dateTime: DateTime, run: suspend () -> T): T {
            return freezeAt(dateTime.millis, run)
        }

        suspend fun <T> freezeAt(timestamp: Long, run: suspend () -> T): T {
            return freezeAt(timestamp).thawAfter(run)
        }

        fun freezeAt(dateTime: DateTime): SuspendFreeze {
            return freezeAt(dateTime.millis)
        }

        fun freezeAt(millis: Long): SuspendFreeze {
            setCurrentMillisFixed(millis)
            return SuspendFreeze()
        }

        fun thaw() {
            setCurrentMillisSystem()
        }
    }
}