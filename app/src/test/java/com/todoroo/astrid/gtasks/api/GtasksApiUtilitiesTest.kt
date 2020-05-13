package com.todoroo.astrid.gtasks.api

import com.google.api.client.util.DateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.*

class GtasksApiUtilitiesTest {
    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultDateTimeZone)
    }

    @Test
    fun testConvertUnixToGoogleCompletionTime() {
        val now = org.tasks.time.DateTime(2014, 1, 8, 8, 53, 20, 109).millis
        assertEquals(now, GtasksApiUtilities.unixTimeToGtasksCompletionTime(now).value)
    }

    @Test
    fun testConvertGoogleCompletedTimeToUnixTime() {
        val now = org.tasks.time.DateTime(2014, 1, 8, 8, 53, 20, 109).millis
        val gtime = DateTime(now)
        assertEquals(now, GtasksApiUtilities.gtasksCompletedTimeToUnixTime(gtime))
    }

    @Test
    fun testConvertDueDateTimeToGoogleDueDate() {
        val now = org.tasks.time.DateTime(2014, 1, 8, 8, 53, 20, 109)
        assertEquals(
                org.tasks.time.DateTime(2014, 1, 8, 0, 0, 0, 0, TimeZone.getTimeZone("GMT")).millis,
                GtasksApiUtilities.unixTimeToGtasksDueDate(now.millis).value)
    }

    @Test
    fun testConvertGoogleDueDateToUnixTime() {
        val googleDueDate = DateTime(
                Date(org.tasks.time.DateTime(2014, 1, 8, 0, 0, 0, 0).millis),
                TimeZone.getTimeZone("GMT"))
        assertEquals(
                org.tasks.time.DateTime(2014, 1, 8, 6, 0, 0, 0).millis, GtasksApiUtilities.gtasksDueTimeToUnixTime(googleDueDate))
    }

    @Test
    fun testConvertToInvalidGtaskTimes() {
        assertNull(GtasksApiUtilities.unixTimeToGtasksCompletionTime(-1))
        assertNull(GtasksApiUtilities.unixTimeToGtasksDueDate(-1))
    }

    @Test
    fun testConvertFromInvalidGtaskTimes() {
        assertEquals(0, GtasksApiUtilities.gtasksCompletedTimeToUnixTime(null))
        assertEquals(0, GtasksApiUtilities.gtasksDueTimeToUnixTime(null))
    }

    companion object {
        private val defaultLocale = Locale.getDefault()
        private val defaultDateTimeZone = TimeZone.getDefault()
    }
}