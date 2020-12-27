package org.tasks

import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils

class SuspendFreeze {

    suspend fun thawAfter(run: suspend () -> Unit) {
        try {
            run()
        } finally {
            thaw()
        }
    }

    companion object {

        suspend fun freezeClock(run: suspend () -> Unit) {
            freezeAt(DateTimeUtils.currentTimeMillis()).thawAfter(run)
        }

        suspend fun freezeAt(dateTime: DateTime, run: suspend () -> Unit) {
            freezeAt(dateTime.millis, run)
        }

        suspend fun freezeAt(timestamp: Long, run: suspend () -> Unit) {
            freezeAt(timestamp).thawAfter(run)
        }

        fun freezeAt(dateTime: DateTime): SuspendFreeze {
            return freezeAt(dateTime.millis)
        }

        fun freezeAt(millis: Long): SuspendFreeze {
            DateTimeUtils.setCurrentMillisFixed(millis)
            return SuspendFreeze()
        }

        fun thaw() {
            DateTimeUtils.setCurrentMillisSystem()
        }
    }
}