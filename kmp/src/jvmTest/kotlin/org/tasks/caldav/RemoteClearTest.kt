package org.tasks.caldav

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.acknowledgesLastReminder
import org.tasks.time.DateTime
import org.tasks.time.endOfMinute

class RemoteClearTest {
    private val firedAt = DateTime(2026, 9, 2, 16, 31, 43)

    private val reminderLast = firedAt.millis.endOfMinute()

    @Test
    fun anAckFromTheMinuteTheReminderFiredClearsIt() {
        val dismissed = DateTime(2026, 9, 2, 16, 31, 59).millis

        assertTrue(acknowledgesLastReminder(dismissed, reminderLast))
    }

    @Test
    fun anAckFromBeforeTheReminderIsStale() {
        val dismissed = DateTime(2026, 9, 2, 16, 30, 59).millis

        assertFalse(acknowledgesLastReminder(dismissed, reminderLast))
    }

    @Test
    fun anAckFromAfterTheReminderClearsIt() {
        val dismissed = DateTime(2026, 9, 2, 16, 32, 5).millis

        assertTrue(acknowledgesLastReminder(dismissed, reminderLast))
    }

    @Test
    fun nothingShowingIsNothingToClear() {
        assertFalse(acknowledgesLastReminder(firedAt.millis, reminderLast = 0L))
    }

    @Test
    fun aTaskThatWasNeverAcknowledgedIsLeftAlone() {
        assertFalse(acknowledgesLastReminder(remoteAck = 0L, reminderLast = reminderLast))
    }
}
