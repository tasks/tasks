package com.todoroo.andlib.utility

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.Freeze
import org.tasks.time.DateTime
import java.time.format.FormatStyle
import java.util.*

@RunWith(AndroidJUnit4::class)
class RelativeDayTest {
    private lateinit var defaultLocale: Locale
    private val now = DateTime(2013, 12, 31, 11, 9, 42, 357)
    
    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        Freeze.freezeAt(now)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        Freeze.thaw()
    }

    @Test
    fun testRelativeDayIsToday() {
        checkRelativeDay(DateTime(), "Today", "Today")
    }

    @Test
    fun testRelativeDayIsTomorrow() {
        checkRelativeDay(DateTime().plusDays(1), "Tomorrow", "Tmrw")
    }

    @Test
    fun testRelativeDayIsYesterday() {
        checkRelativeDay(DateTime().minusDays(1), "Yesterday", "Yest")
    }

    @Test
    fun testRelativeDayTwo() {
        checkRelativeDay(DateTime().minusDays(2), "Sunday", "Sun")
        checkRelativeDay(DateTime().plusDays(2), "Thursday", "Thu")
    }

    @Test
    fun testRelativeDaySix() {
        checkRelativeDay(DateTime().minusDays(6), "Wednesday", "Wed")
        checkRelativeDay(DateTime().plusDays(6), "Monday", "Mon")
    }

    @Test
    fun testRelativeDayOneWeek() {
        checkRelativeDay(DateTime().minusDays(7), "December 24", "Dec 24")
    }

    @Test
    fun testRelativeDayOneWeekNextYear() {
        checkRelativeDay(DateTime().plusDays(7), "January 7, 2014", "Jan 7, 2014")
    }

    private fun checkRelativeDay(now: DateTime, full: String, abbreviated: String) {
        assertEquals(
                full,
                DateUtilities.getRelativeDay(ApplicationProvider.getApplicationContext(), now.millis, Locale.US, FormatStyle.LONG))
        assertEquals(
                abbreviated,
                DateUtilities.getRelativeDay(ApplicationProvider.getApplicationContext(), now.millis, Locale.US, FormatStyle.MEDIUM))
    }
}