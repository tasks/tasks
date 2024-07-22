/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.SuspendFreeze
import org.tasks.SuspendFreeze.Companion.freezeAt
import org.tasks.TestUtilities.withLocale
import org.tasks.date.DateTimeUtils
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.extensions.Context.is24HourOverride
import org.tasks.kmp.formatDayOfWeek
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.TextStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.time.DateTime
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class DateUtilitiesTest {
    @After
    fun after() {
        is24HourOverride = null
    }

    @Test
    fun testGet24HourTime() {
        is24HourOverride = true
        assertEquals("09:05", getTimeString(DateTime(2014, 1, 4, 9, 5, 36).millis, is24HourFormat))
        assertEquals("13:00", getTimeString(DateTime(2014, 1, 4, 13, 0, 1).millis, is24HourFormat))
    }

    @Test
    fun testGetTime() {
        is24HourOverride = false
        assertEquals("9:05 AM", getTimeString(DateTime(2014, 1, 4, 9, 5, 36).millis, is24HourFormat))
        assertEquals("1:05 PM", getTimeString(DateTime(2014, 1, 4, 13, 5, 36).millis, is24HourFormat))
    }

    @Test
    fun testGetTimeWithNoMinutes() {
        is24HourOverride = false
        assertEquals("1 PM", getTimeString(DateTime(2014, 1, 4, 13, 0, 59).millis, is24HourFormat)) // derp?
    }

    @Test
    fun testGetDateStringWithYear() = runBlocking {
        assertEquals("Jan 4, 2014", getRelativeDay(DateTime(2014, 1, 4, 0, 0, 0).millis))
    }

    @Test
    fun testGetDateStringHidingYear() = runBlocking {
        freezeAt(DateTimeUtils.newDate(2014, 2, 1)) {
            assertEquals("Jan 1", getRelativeDay(DateTime(2014, 1, 1).millis))
        }
    }

    @Test
    fun testGetDateStringWithDifferentYear() = runBlocking {
        freezeAt(DateTimeUtils.newDate(2013, 12, 1)) {
            assertEquals("Jan 1, 2014", getRelativeDay(DateTime(2014, 1, 1, 0, 0, 0).millis))
        }
    }

    @Test
    fun testGetWeekdayLongString() = withLocale(Locale.US) {
        assertEquals("Sunday", formatDayOfWeek(DateTimeUtils.newDate(2013, 12, 29).millis, TextStyle.FULL))
        assertEquals("Monday", formatDayOfWeek(DateTimeUtils.newDate(2013, 12, 30).millis, TextStyle.FULL))
        assertEquals("Tuesday", formatDayOfWeek(DateTimeUtils.newDate(2013, 12, 31).millis, TextStyle.FULL))
        assertEquals("Wednesday", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 1).millis, TextStyle.FULL))
        assertEquals("Thursday", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 2).millis, TextStyle.FULL))
        assertEquals("Friday", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 3).millis, TextStyle.FULL))
        assertEquals("Saturday", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 4).millis, TextStyle.FULL))
    }

    @Test
    fun testGetWeekdayShortString() = withLocale(Locale.US) {
        assertEquals("Sun", formatDayOfWeek(DateTimeUtils.newDate(2013, 12, 29).millis, TextStyle.SHORT))
        assertEquals("Mon", formatDayOfWeek(DateTimeUtils.newDate(2013, 12, 30).millis, TextStyle.SHORT))
        assertEquals("Tue", formatDayOfWeek(DateTimeUtils.newDate(2013, 12, 31).millis, TextStyle.SHORT))
        assertEquals("Wed", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 1).millis, TextStyle.SHORT))
        assertEquals("Thu", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 2).millis, TextStyle.SHORT))
        assertEquals("Fri", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 3).millis, TextStyle.SHORT))
        assertEquals("Sat", formatDayOfWeek(DateTimeUtils.newDate(2014, 1, 4).millis, TextStyle.SHORT))
    }

    @Test
    fun getRelativeFullDate() = withLocale(Locale.US) {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                "Sunday, January 14",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun getRelativeFullDateWithYear() = withLocale(Locale.US) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                "Sunday, January 14, 2018",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun getRelativeFullDateTime() = withLocale(Locale.US) {
        freezeAt(DateTime(2018, 1, 1)) {
            assertMatches(
                "Sunday, January 14( at)? 1:43 PM",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 43, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    @Ignore("Fails on CI - need to investigate")
    fun getRelativeDateTimeWithAlwaysDisplayFullDateOption() = withLocale(Locale.US) {
        freezeAt(DateTime(2020, 1, 1)) {
            assertMatches(
                "Thursday, January 2 at 11:50 AM",
                getRelativeDateTime(DateTime(2020, 1, 2, 11, 50, 1).millis, is24HourFormat, DateStyle.FULL, true, false)
            )
        }
    }

    @Test
    fun getRelativeFullDateTimeWithYear() = withLocale(Locale.US) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                "Sunday, January 14, 2018( at)? 11:50 AM",
                getRelativeDateTime(DateTime(2018, 1, 14, 11, 50, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun getRelativeDayWithAlwaysDisplayFullDateOption() = withLocale(Locale.US) {
        freezeAt(DateTime(2020, 1, 1)) {
            assertEquals(
                    "Thursday, January 2",
                    getRelativeDay(DateTime(2020, 1, 2, 11, 50, 1).millis, DateStyle.FULL, alwaysDisplayFullDate = true, lowercase = true)
            )
        }
    }

    @Test
    fun getRelativeDayWithoutAlwaysDisplayFullDateOption() = withLocale(Locale.US) {
        freezeAt(DateTime(2020, 1, 1)) {
            assertEquals(
                    "tomorrow",
                    getRelativeDay(DateTime(2020, 1, 2, 11, 50, 1).millis, DateStyle.FULL, lowercase = true)
            )
        }
    }

    @Test
    fun germanDateNoYear() = withLocale(Locale.GERMAN) {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                "Sonntag, 14. Januar",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun germanDateWithYear() = withLocale(Locale.GERMAN) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                "Sonntag, 14. Januar 2018",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun koreanDateNoYear() = withLocale(Locale.KOREAN) {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                "1월 14일 일요일",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun koreanDateWithYear() = withLocale(Locale.KOREAN) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                "2018년 1월 14일 일요일",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun japaneseDateNoYear() = withLocale(Locale.JAPANESE) {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                "1月14日日曜日",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun japaneseDateWithYear() = withLocale(Locale.JAPANESE) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                "2018年1月14日日曜日",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun chineseDateNoYear() = withLocale(Locale.CHINESE) {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                "1月14日星期日",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun chineseDateWithYear() = withLocale(Locale.CHINESE) {
        SuspendFreeze.freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                "2018年1月14日星期日",
                getRelativeDateTime(DateTime(2018, 1, 14).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun chineseDateTimeNoYear() = withLocale(Locale.CHINESE) {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                "1月14日星期日 上午11:53",
                getRelativeDateTime(DateTime(2018, 1, 14, 11, 53, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun chineseDateTimeWithYear() = withLocale(Locale.CHINESE) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                "2018年1月14日星期日 下午1:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun frenchDateTimeWithYear() = withLocale(Locale.FRENCH) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                "dimanche 14 janvier 2018( à)? 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun indiaDateTimeWithYear() = withLocale(Locale.forLanguageTag("hi-IN")) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                "रविवार, 14 जनवरी 2018( को)? 1:45 pm",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun russiaDateTimeNoYear() = withLocale(Locale.forLanguageTag("ru")) {
        freezeAt(DateTime(2018, 12, 12)) {
            assertMatches(
                "воскресенье, 14 января,? 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun russiaDateTimeWithYear() = withLocale(Locale.forLanguageTag("ru")) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                "воскресенье, 14 января 2018 г.,? 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun brazilDateTimeNoYear() = withLocale(Locale.forLanguageTag("pt-br")) {
        freezeAt(DateTime(2018, 12, 12)) {
            assertEquals(
                "domingo, 14 de janeiro 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun brazilDateTimeWithYear() = withLocale(Locale.forLanguageTag("pt-br")) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                "domingo, 14 de janeiro de 2018 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun spainDateTimeNoYear() = withLocale(Locale.forLanguageTag("es")) {
        freezeAt(DateTime(2018, 12, 12)) {
            assertMatches(
                "domingo, 14 de enero,? 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun spainDateTimeWithYear() = withLocale(Locale.forLanguageTag("es")) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                "domingo, 14 de enero de 2018,? 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun hebrewDateTimeNoYear() = withLocale(Locale.forLanguageTag("iw")) {
        freezeAt(DateTime(2018, 12, 12)) {
            assertMatches(
                "יום ראשון, 14 בינואר( בשעה)? 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    @Test
    fun hebrewDateTimeWithYear() = withLocale(Locale.forLanguageTag("iw")) {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                "יום ראשון, 14 בינואר 2018( בשעה)? 13:45",
                getRelativeDateTime(DateTime(2018, 1, 14, 13, 45, 1).millis, is24HourFormat, DateStyle.FULL)
            )
        }
    }

    private fun assertMatches(regex: String, actual: String) =
        assertTrue("expected=$regex\nactual=$actual", actual.matches(Regex(regex)))

    private val is24HourFormat: Boolean
        get() = InstrumentationRegistry.getInstrumentation().targetContext.is24HourFormat
}