/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import android.content.res.Configuration;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;

import org.tasks.Snippet;
import org.tasks.time.DateTime;

import java.util.Locale;

import static com.todoroo.andlib.utility.DateUtilities.addCalendarMonthsToUnixtime;
import static com.todoroo.andlib.utility.DateUtilities.getDateString;
import static com.todoroo.andlib.utility.DateUtilities.getDateStringHideYear;
import static com.todoroo.andlib.utility.DateUtilities.getStartOfDay;
import static com.todoroo.andlib.utility.DateUtilities.getTimeString;
import static com.todoroo.andlib.utility.DateUtilities.getWeekday;
import static com.todoroo.andlib.utility.DateUtilities.getWeekdayShort;
import static com.todoroo.andlib.utility.DateUtilities.oneMonthFromNow;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class DateUtilitiesTest extends AndroidTestCase {

    private static Locale defaultLocale;

    @Override
    public void setUp() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @Override
    public void tearDown() {
        DateUtilities.is24HourOverride = null;
        Locale.setDefault(defaultLocale);
    }

    private void setLocale(Locale locale) {
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        getContext().getResources().updateConfiguration(config, metrics);
    }

    public void forEachLocale(Runnable r) {
        Locale[] locales = Locale.getAvailableLocales();
        for(Locale locale : locales) {
            setLocale(locale);

            r.run();
        }
    }

    public void testTimeString() {
        forEachLocale(new Runnable() {
            public void run() {
                DateTime d = newDateTime();

                DateUtilities.is24HourOverride = false;
                for (int i = 0; i < 24; i++) {
                    d = d.withHourOfDay(i);
                    getTimeString(getContext(), d);
                }

                DateUtilities.is24HourOverride = true;
                for (int i = 0; i < 24; i++) {
                    d = d.withHourOfDay(i);
                    getTimeString(getContext(), d);
                }
            }
        });
    }

    public void testDateString() {
        forEachLocale(new Runnable() {
            public void run() {
                DateTime d = newDateTime();

                for (int i = 0; i < 12; i++) {
                    d = d.withMonthOfYear(i);
                    getDateString(d);
                }
            }
        });
    }

    public void testGet24HourTime() {
        DateUtilities.is24HourOverride = true;
        assertEquals("09:05", getTimeString(null, new DateTime(2014, 1, 4, 9, 5, 36)));
        assertEquals("13:00", getTimeString(null, new DateTime(2014, 1, 4, 13, 0, 1)));
    }

    public void testGetTime() {
        DateUtilities.is24HourOverride = false;
        assertEquals("9:05 AM", getTimeString(null, new DateTime(2014, 1, 4, 9, 5, 36)));
        assertEquals("1:05 PM", getTimeString(null, new DateTime(2014, 1, 4, 13, 5, 36)));
    }

    public void testGetTimeWithNoMinutes() {
        DateUtilities.is24HourOverride = false;
        assertEquals("1 PM", getTimeString(null, new DateTime(2014, 1, 4, 13, 0, 59))); // derp?
    }

    public void testGetDateStringWithYear() {
        assertEquals("Jan 4, 2014", getDateString(new DateTime(2014, 1, 4, 0, 0, 0)));
    }

    public void testGetDateStringHidingYear() {
        freezeAt(newDate(2014, 1, 1)).thawAfter(new Snippet() {{
            assertEquals("Jan 1", getDateStringHideYear(newDateTime()));
        }});
    }

    public void testGetDateStringWithDifferentYear() {
        freezeAt(newDate(2013, 12, 31)).thawAfter(new Snippet() {{
            assertEquals("Jan 1\n2014", getDateStringHideYear(new DateTime(2014, 1, 1, 0, 0, 0)));
        }});
    }

    public void testOneMonthFromStartOfDecember() {
        DateTime now = new DateTime(2013, 12, 1, 12, 19, 45, 192);
        final long expected = new DateTime(2014, 1, 1, 12, 19, 45, 192).getMillis();

        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(expected, oneMonthFromNow());
        }});
    }

    public void testOneMonthFromEndOfDecember() {
        DateTime now = new DateTime(2013, 12, 31, 16, 31, 20, 597);
        final long expected = new DateTime(2014, 1, 31, 16, 31, 20, 597).getMillis();

        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(expected, oneMonthFromNow());
        }});
    }

    public void testGetSixMonthsFromEndOfDecember() {
        final DateTime now = new DateTime(2013, 12, 31, 17, 17, 32, 900);
        final long expected = new DateTime(2014, 7, 1, 17, 17, 32, 900).getMillis();

        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(expected, addCalendarMonthsToUnixtime(now.getMillis(), 6));
        }});
    }

    public void testOneMonthFromEndOfJanuary() {
        DateTime now = new DateTime(2014, 1, 31, 12, 54, 33, 175);
        final long expected = new DateTime(2014, 3, 3, 12, 54, 33, 175).getMillis();

        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(expected, oneMonthFromNow());
        }});
    }

    public void testOneMonthFromEndOfFebruary() {
        DateTime now = new DateTime(2014, 2, 28, 9, 19, 7, 990);
        final long expected = new DateTime(2014, 3, 28, 9, 19, 7, 990).getMillis();

        freezeAt(now).thawAfter(new Snippet() {{
            assertEquals(expected, oneMonthFromNow());
        }});
    }

    public void testShouldGetStartOfDay() {
        DateTime now = new DateTime(2014, 1, 3, 10, 41, 41, 520);
        assertEquals(
                now.startOfDay().getMillis(),
                getStartOfDay(now.getMillis()));
    }

    public void testGetWeekdayLongString() {
        assertEquals("Sunday", getWeekday(newDate(2013, 12, 29)));
        assertEquals("Monday", getWeekday(newDate(2013, 12, 30)));
        assertEquals("Tuesday", getWeekday(newDate(2013, 12, 31)));
        assertEquals("Wednesday", getWeekday(newDate(2014, 1, 1)));
        assertEquals("Thursday", getWeekday(newDate(2014, 1, 2)));
        assertEquals("Friday", getWeekday(newDate(2014, 1, 3)));
        assertEquals("Saturday", getWeekday(newDate(2014, 1, 4)));
    }

    public void testGetWeekdayShortString() {
        assertEquals("Sun", getWeekdayShort(newDate(2013, 12, 29)));
        assertEquals("Mon", getWeekdayShort(newDate(2013, 12, 30)));
        assertEquals("Tue", getWeekdayShort(newDate(2013, 12, 31)));
        assertEquals("Wed", getWeekdayShort(newDate(2014, 1, 1)));
        assertEquals("Thu", getWeekdayShort(newDate(2014, 1, 2)));
        assertEquals("Fri", getWeekdayShort(newDate(2014, 1, 3)));
        assertEquals("Sat", getWeekdayShort(newDate(2014, 1, 4)));
    }

    public void testAddMonthsToTimestamp() {
        assertEquals(newDate(2014, 1, 1).getMillis(), addCalendarMonthsToUnixtime(newDate(2013, 12, 1).getMillis(), 1));
        assertEquals(newDate(2014, 12, 31).getMillis(), addCalendarMonthsToUnixtime(newDate(2013, 12, 31).getMillis(), 12));
    }

    public void testAddMonthsWithLessDays() {
        assertEquals(newDate(2014, 3, 3).getMillis(), addCalendarMonthsToUnixtime(newDate(2013, 12, 31).getMillis(), 2));
    }

    public void testAddMonthsWithMoreDays() {
        assertEquals(newDate(2014, 1, 30).getMillis(), addCalendarMonthsToUnixtime(newDate(2013, 11, 30).getMillis(), 2));
    }
}
