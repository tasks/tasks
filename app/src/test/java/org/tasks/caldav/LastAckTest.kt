package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.TestUtilities.withTZ
import org.tasks.caldav.iCalendar.Companion.lastAck
import org.tasks.caldav.iCalendar.Companion.snooze
import org.tasks.time.DateTime

class LastAckTest {
    @Test
    fun writeAcknowledgedTime() = withTZ(CHICAGO) {
        val remote = Task()

        remote.lastAck = DateTime(2021, 2, 10, 9, 22, 35).millis

        assertEquals("20210210T152235Z", remote.property(MOZ_LASTACK))
    }

    @Test
    fun readAcknowledgedTime() = withTZ(CHICAGO) {
        val remote = Task()
        remote.lastAck = DateTime(2021, 2, 10, 9, 22, 35).millis

        assertEquals(DateTime(2021, 2, 10, 9, 22, 35).millis, remote.lastAck)
    }

    @Test
    fun neverAcknowledgedWritesNothing() = withTZ(CHICAGO) {
        val remote = Task()

        remote.lastAck = 0

        assertNull(remote.property(MOZ_LASTACK))
    }

    @Test
    fun keepExistingAcknowledgedTimeWhenNothingToWrite() = withTZ(CHICAGO) {
        val remote = Task()
        remote.lastAck = DateTime(2021, 2, 10, 9, 22, 35).millis

        remote.lastAck = 0

        assertEquals("20210210T152235Z", remote.property(MOZ_LASTACK))
    }

    @Test
    fun snoozingDoesNotAcknowledgeByItself() = withTZ(CHICAGO) {
        val remote = Task().apply {
            lastModified = DateTime(2021, 2, 10, 9, 22, 35).millis
        }

        remote.snooze = DateTime(2099, 2, 10, 9, 52, 35).millis

        assertNull(remote.property(MOZ_LASTACK))
    }

    private fun Task.property(name: String) =
        unknownProperties.find { it.name.equals(name, true) }?.value

    companion object {
        private const val MOZ_LASTACK = "X-MOZ-LASTACK"
        private const val CHICAGO = "America/Chicago"
    }
}
