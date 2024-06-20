/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.date.DateTimeUtils
import org.tasks.time.DateTime
import java.time.format.FormatStyle
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class DateUtilitiesTest {
    @After
    fun after() {
        DateUtilities.is24HourOverride = null
    }

    @Test
    fun testGet24HourTime() {
        DateUtilities.is24HourOverride = true
        assertEquals("09:05", DateUtilities.getTimeString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 4, 9, 5, 36)))
        assertEquals("13:00", DateUtilities.getTimeString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 4, 13, 0, 1)))
    }

    @Test
    fun testGetTime() {
        DateUtilities.is24HourOverride = false
        assertEquals("9:05 AM", DateUtilities.getTimeString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 4, 9, 5, 36)))
        assertEquals("1:05 PM", DateUtilities.getTimeString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 4, 13, 5, 36)))
    }

    @Test
    fun testGetTimeWithNoMinutes() {
        DateUtilities.is24HourOverride = false
        assertEquals("1 PM", DateUtilities.getTimeString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 4, 13, 0, 59))) // derp?
    }

    @Test
    fun testGetDateStringWithYear() {
        assertEquals("Jan 4, 2014", DateUtilities.getDateString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 4, 0, 0, 0)))
    }

    @Test
    fun testGetDateStringHidingYear() {
        freezeAt(DateTimeUtils.newDate(2014, 2, 1)) {
            assertEquals("Jan 1", DateUtilities.getDateString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 1)))
        }
    }

    @Test
    fun testGetDateStringWithDifferentYear() {
        freezeAt(DateTimeUtils.newDate(2013, 12, 1)) {
            assertEquals("Jan 1, 2014", DateUtilities.getDateString(ApplicationProvider.getApplicationContext(), DateTime(2014, 1, 1, 0, 0, 0)))
        }
    }

    @Test
    fun testGetWeekdayLongString() {
        assertEquals("Sunday", DateUtilities.getWeekday(DateTimeUtils.newDate(2013, 12, 29), Locale.US))
        assertEquals("Monday", DateUtilities.getWeekday(DateTimeUtils.newDate(2013, 12, 30), Locale.US))
        assertEquals("Tuesday", DateUtilities.getWeekday(DateTimeUtils.newDate(2013, 12, 31), Locale.US))
        assertEquals("Wednesday", DateUtilities.getWeekday(DateTimeUtils.newDate(2014, 1, 1), Locale.US))
        assertEquals("Thursday", DateUtilities.getWeekday(DateTimeUtils.newDate(2014, 1, 2), Locale.US))
        assertEquals("Friday", DateUtilities.getWeekday(DateTimeUtils.newDate(2014, 1, 3), Locale.US))
        assertEquals("Saturday", DateUtilities.getWeekday(DateTimeUtils.newDate(2014, 1, 4), Locale.US))
    }

    @Test
    fun testGetWeekdayShortString() {
        assertEquals("Sun", DateUtilities.getWeekdayShort(DateTimeUtils.newDate(2013, 12, 29), Locale.US))
        assertEquals("Mon", DateUtilities.getWeekdayShort(DateTimeUtils.newDate(2013, 12, 30), Locale.US))
        assertEquals("Tue", DateUtilities.getWeekdayShort(DateTimeUtils.newDate(2013, 12, 31), Locale.US))
        assertEquals("Wed", DateUtilities.getWeekdayShort(DateTimeUtils.newDate(2014, 1, 1), Locale.US))
        assertEquals("Thu", DateUtilities.getWeekdayShort(DateTimeUtils.newDate(2014, 1, 2), Locale.US))
        assertEquals("Fri", DateUtilities.getWeekdayShort(DateTimeUtils.newDate(2014, 1, 3), Locale.US))
        assertEquals("Sat", DateUtilities.getWeekdayShort(DateTimeUtils.newDate(2014, 1, 4), Locale.US))
    }

    @Test
    fun getRelativeFullDate() {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                    "Sunday, January 14",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.US,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun getRelativeFullDateWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                    "Sunday, January 14, 2018",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.US,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun getRelativeFullDateTime() {
        freezeAt(DateTime(2018, 1, 1)) {
            assertMatches(
                    "Sunday, January 14( at)? 1:43 PM",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 43, 1).millis,
                            Locale.US,
                            FormatStyle.FULL))
        }
    }

    @Test
    @Ignore("Fails on CI - need to investigate")
    fun getRelativeDateTimeWithAlwaysDisplayFullDateOption() {
        freezeAt(DateTime(2020, 1, 1)) {
            assertMatches(
                    "Thursday, January 2 at 11:50 AM",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2020, 1, 2, 11, 50, 1).millis,
                            Locale.US,
                            FormatStyle.FULL,
                            true,
                            false
                    ))
        }
    }

    @Test
    fun getRelativeFullDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                    "Sunday, January 14, 2018( at)? 11:50 AM",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 11, 50, 1).millis,
                            Locale.US,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun getRelativeDayWithAlwaysDisplayFullDateOption() {
        freezeAt(DateTime(2020, 1, 1)) {
            assertEquals(
                    "Thursday, January 2",
                    DateUtilities.getRelativeDay(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2020, 1, 2, 11, 50, 1).millis,
                            Locale.US,
                            FormatStyle.FULL,
                            true,
                            true
                    )
            )
        }
    }

    @Test
    fun getRelativeDayWithoutAlwaysDisplayFullDateOption() {
        freezeAt(DateTime(2020, 1, 1)) {
            assertEquals(
                    "tomorrow",
                    DateUtilities.getRelativeDay(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2020, 1, 2, 11, 50, 1).millis,
                            Locale.US,
                            FormatStyle.FULL,
                            false,
                            true
                    )
            )
        }
    }

    @Test
    fun germanDateNoYear() {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                    "Sonntag, 14. Januar",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.GERMAN,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun germanDateWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                    "Sonntag, 14. Januar 2018",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.GERMAN,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun koreanDateNoYear() {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                    "1월 14일 일요일",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.KOREAN,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun koreanDateWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                    "2018년 1월 14일 일요일",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.KOREAN,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun japaneseDateNoYear() {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                    "1月14日日曜日",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.JAPANESE,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun japaneseDateWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                    "2018年1月14日日曜日",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.JAPANESE,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun chineseDateNoYear() {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                    "1月14日星期日",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.CHINESE,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun chineseDateWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                    "2018年1月14日星期日",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14).millis,
                            Locale.CHINESE,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun chineseDateTimeNoYear() {
        freezeAt(DateTime(2018, 1, 1)) {
            assertEquals(
                    "1月14日星期日 上午11:53",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 11, 53, 1).millis,
                            Locale.CHINESE,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun chineseDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                    "2018年1月14日星期日 下午1:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.CHINESE,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun frenchDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                    "dimanche 14 janvier 2018( à)? 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.FRENCH,
                            FormatStyle.FULL))
        }
    }

    @Test
    fun indiaDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                    "रविवार, 14 जनवरी 2018( को)? 1:45 pm",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("hi-IN"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun russiaDateTimeNoYear() {
        freezeAt(DateTime(2018, 12, 12)) {
            assertMatches(
                    "воскресенье, 14 января,? 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("ru"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun russiaDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                    "воскресенье, 14 января 2018 г.,? 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("ru"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun brazilDateTimeNoYear() {
        freezeAt(DateTime(2018, 12, 12)) {
            assertEquals(
                    "domingo, 14 de janeiro 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("pt-br"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun brazilDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertEquals(
                    "domingo, 14 de janeiro de 2018 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("pt-br"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun spainDateTimeNoYear() {
        freezeAt(DateTime(2018, 12, 12)) {
            assertMatches(
                    "domingo, 14 de enero,? 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("es"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun spainDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {
            assertMatches(
                    "domingo, 14 de enero de 2018,? 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("es"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun hebrewDateTimeNoYear() {
        freezeAt(DateTime(2018, 12, 12)) {
            assertMatches(
                    "יום ראשון, 14 בינואר( בשעה)? 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("iw"),
                            FormatStyle.FULL))
        }
    }

    @Test
    fun hebrewDateTimeWithYear() {
        freezeAt(DateTime(2017, 12, 12)) {

            assertMatches(
                    "יום ראשון, 14 בינואר 2018( בשעה)? 13:45",
                    DateUtilities.getRelativeDateTime(
                            ApplicationProvider.getApplicationContext(),
                            DateTime(2018, 1, 14, 13, 45, 1).millis,
                            Locale.forLanguageTag("iw"),
                            FormatStyle.FULL))
        }
    }

    private fun assertMatches(regex: String, actual: String) =
        assertTrue("expected=$regex\nactual=$actual", actual.matches(Regex(regex)))
}