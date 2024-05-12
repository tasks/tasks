package org.tasks.preferences

import android.annotation.SuppressLint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.tasks.data.entity.Task.Companion.NOTIFY_AFTER_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_DEADLINE
import org.tasks.data.entity.Task.Companion.NOTIFY_AT_START
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.R
import org.tasks.TestUtilities.newPreferences
import org.tasks.time.DateTime
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PreferenceTests {
    private lateinit var preferences: Preferences

    @Before
    fun setUp() {
        preferences = newPreferences(ApplicationProvider.getApplicationContext())
        preferences.clear()
        preferences.setBoolean(R.string.p_rmd_enable_quiet, true)
    }

    @Test
    fun testNotQuietWhenQuietHoursDisabled() {
        preferences.setBoolean(R.string.p_rmd_enable_quiet, false)
        setQuietHoursStart(22)
        setQuietHoursEnd(10)
        val dueDate = DateTime(2015, 12, 29, 8, 0, 1).millis
        assertEquals(dueDate, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testIsQuietAtStartOfQuietHoursNoWrap() {
        setQuietHoursStart(18)
        setQuietHoursEnd(19)
        val dueDate = DateTime(2015, 12, 29, 18, 0, 1).millis
        assertEquals(
                DateTime(2015, 12, 29, 19, 0).millis, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testIsQuietAtStartOfQuietHoursWrap() {
        setQuietHoursStart(22)
        setQuietHoursEnd(10)
        val dueDate = DateTime(2015, 12, 29, 22, 0, 1).millis
        assertEquals(
                DateTime(2015, 12, 30, 10, 0).millis, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testAdjustForQuietHoursNightWrap() {
        setQuietHoursStart(22)
        setQuietHoursEnd(10)
        val dueDate = DateTime(2015, 12, 29, 23, 30).millis
        assertEquals(
                DateTime(2015, 12, 30, 10, 0).millis, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testAdjustForQuietHoursMorningWrap() {
        setQuietHoursStart(22)
        setQuietHoursEnd(10)
        val dueDate = DateTime(2015, 12, 30, 7, 15).millis
        assertEquals(
                DateTime(2015, 12, 30, 10, 0).millis, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testAdjustForQuietHoursWhenStartAndEndAreSame() {
        setQuietHoursStart(18)
        setQuietHoursEnd(18)
        val dueDate = DateTime(2015, 12, 29, 18, 0, 0).millis
        assertEquals(dueDate, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testIsNotQuietAtEndOfQuietHoursNoWrap() {
        setQuietHoursStart(17)
        setQuietHoursEnd(18)
        val dueDate = DateTime(2015, 12, 29, 18, 0).millis
        assertEquals(dueDate, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testIsNotQuietAtEndOfQuietHoursWrap() {
        setQuietHoursStart(22)
        setQuietHoursEnd(10)
        val dueDate = DateTime(2015, 12, 29, 10, 0).millis
        assertEquals(dueDate, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testIsNotQuietBeforeNoWrap() {
        setQuietHoursStart(17)
        setQuietHoursEnd(18)
        val dueDate = DateTime(2015, 12, 29, 11, 30).millis
        assertEquals(dueDate, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testIsNotQuietAfterNoWrap() {
        setQuietHoursStart(17)
        setQuietHoursEnd(18)
        val dueDate = DateTime(2015, 12, 29, 22, 15).millis
        assertEquals(dueDate, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testIsNotQuietWrap() {
        setQuietHoursStart(22)
        setQuietHoursEnd(10)
        val dueDate = DateTime(2015, 12, 29, 13, 45).millis
        assertEquals(dueDate, preferences.adjustForQuietHours(dueDate))
    }

    @Test
    fun testDefaultReminders() {
        assertEquals(0, defaultReminders())
        assertEquals(2, defaultReminders(NOTIFY_AT_DEADLINE))
        assertEquals(4, defaultReminders(NOTIFY_AFTER_DEADLINE))
        assertEquals(6, defaultReminders(NOTIFY_AT_DEADLINE, NOTIFY_AFTER_DEADLINE))
        assertEquals(32, defaultReminders(NOTIFY_AT_START))
        assertEquals(38, defaultReminders(NOTIFY_AT_START, NOTIFY_AT_DEADLINE, NOTIFY_AFTER_DEADLINE))
    }

    private fun setQuietHoursStart(hour: Int) {
        preferences.setInt(R.string.p_rmd_quietStart, hour * MILLIS_PER_HOUR)
    }

    private fun setQuietHoursEnd(hour: Int) {
        preferences.setInt(R.string.p_rmd_quietEnd, hour * MILLIS_PER_HOUR)
    }

    private fun defaultReminders(vararg values: Int): Int {
        preferences.setStringSet(
            R.string.p_default_reminders_key,
            values.map { it.toString() }.toSet()
        )
        return preferences.defaultReminders
    }

    companion object {
        @SuppressLint("NewApi")
        private val MILLIS_PER_HOUR = TimeUnit.HOURS.toMillis(1).toInt()
    }
}