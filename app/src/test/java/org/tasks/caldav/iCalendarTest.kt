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
    fun oldNextcloudDoesNotSupportReminderSync() {
        assertFalse(TestUtilities.readFile("nextcloud/basic_due_date.txt").supportsReminders())
    }

    @Test
    fun nextcloudBefore_0_17_0_DoesNotSupportReminderSync() {
        assertFalse("-//Nextcloud Tasks v0.16.1".supportsReminders())
    }

    @Test
    fun nextcloud_0_17_0_SupportsReminderSync() {
        assertTrue("-//Nextcloud Tasks v0.17.0".supportsReminders())
    }

    @Test
    fun recentNextcloudSupportsReminderSync() {
        assertTrue("-//Nextcloud Tasks v0.18.1".supportsReminders())
    }

    @Test
    fun unversionedNextcloudAssumedToSupportReminderSync() {
        assertTrue("-//Nextcloud Tasks".supportsReminders())
    }
}