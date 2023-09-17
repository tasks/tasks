package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.TestUtilities
import org.tasks.caldav.iCalendar.Companion.prodId
import org.tasks.caldav.iCalendar.Companion.supportsReminders

class iCalendarTest {
    @Test
    fun parseProdId() {
        assertEquals(
            "-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN",
            TestUtilities.readFile("thunderbird/basic_due_date.txt").prodId()
        )
    }

    @Test
    fun thunderbirdSupportsReminderSync() {
        assertTrue(TestUtilities.readFile("thunderbird/basic_due_date.txt").supportsReminders())
    }

    @Test
    fun nextcloudDoesNotSupportReminderSync() {
        assertFalse(TestUtilities.readFile("nextcloud/basic_due_date.txt").supportsReminders())
    }
}