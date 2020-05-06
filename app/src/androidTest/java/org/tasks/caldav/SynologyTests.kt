package org.tasks.caldav

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.TestUtilities.vtodo
import java.util.*

@RunWith(AndroidJUnit4::class)
class SynologyTests {
    private val defaultTimeZone = TimeZone.getDefault()

    @Before
    fun before() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"))
    }

    @After
    fun after() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun completedWithoutDueDate() {
        assertTrue(vtodo("synology/complete_no_due_date.txt").isCompleted)
    }

    @Test
    fun completedWithDueDate() {
        assertTrue(vtodo("synology/complete_with_date.txt").isCompleted)
    }

    @Test
    fun completedWithDateTime() {
        assertTrue(vtodo("synology/complete_with_date_time.txt").isCompleted)
    }
}