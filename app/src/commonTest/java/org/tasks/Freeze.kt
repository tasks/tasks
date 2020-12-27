package org.tasks

import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils

class Freeze {
    fun thawAfter(run: () -> Unit) {
        try {
            run()
        } finally {
            thaw()
        }
    }

    companion object {

        fun freezeClock(run: () -> Unit) {
            freezeAt(DateTimeUtils.currentTimeMillis()).thawAfter(run)
        }

        fun freezeAt(dateTime: DateTime, run: () -> Unit) {
            freezeAt(dateTime.millis, run)
        }

        fun freezeAt(timestamp: Long, run: () -> Unit) {
            freezeAt(timestamp).thawAfter(run)
        }

        fun freezeAt(dateTime: DateTime): Freeze {
            return freezeAt(dateTime.millis)
        }

        fun freezeAt(millis: Long): Freeze {
            DateTimeUtils.setCurrentMillisFixed(millis)
            return Freeze()
        }

        fun thaw() {
            DateTimeUtils.setCurrentMillisSystem()
        }
    }
}