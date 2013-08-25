/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.todoroo.andlib.test.TodorooTestCase;

public class DateUtilitiesTest extends TodorooTestCase {

    public void set24Hour(boolean is24) {
        DateUtilities.is24HourOverride = is24;
    }

    public void testTimeString() {
        forEachLocale(new Runnable() {
            public void run() {
                Date d = new Date();

                set24Hour(false);
                for(int i = 0; i < 24; i++) {
                    d.setHours(i);
                    DateUtilities.getTimeString(getContext(), d);
                }

                set24Hour(true);
                for(int i = 0; i < 24; i++) {
                    d.setHours(i);
                    DateUtilities.getTimeString(getContext(), d);
                }
            }
        });
    }

    public void testDateString() {
        forEachLocale(new Runnable() {
            public void run() {
                Date d = new Date();

                for(int i = 0; i < 12; i++) {
                    d.setMonth(i);
                    DateUtilities.getDateString(getContext(), d);
                }
            }
        });

    }

    public void testWeekdayString() {
        forEachLocale(new Runnable() {
            public void run() {
                Date d = new Date();

                for(int i = 0; i < 7; i++) {
                    d.setDate(i);
                    DateUtilities.getDateStringWithWeekday(getContext(), d);
                }
            }
        });

    }

    public void testParseISO8601() {
        String withTime = "2013-01-28T13:17:02+00:00";
        long date;
        Calendar cal = Calendar.getInstance();
        try {
            date = DateUtilities.parseIso8601(withTime);
            cal.setTimeInMillis(date);
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        } catch (ParseException e) {
            e.printStackTrace();
            fail("Parse exception");
        }
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.MONTH));
        assertEquals(28, cal.get(Calendar.DATE));
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(17, cal.get(Calendar.MINUTE));
        assertEquals(2, cal.get(Calendar.SECOND));
    }

    public void testParseISO8601NoTime() {
        String withTime = "2013-01-28";
        long date;
        Calendar cal = Calendar.getInstance();
        try {
            date = DateUtilities.parseIso8601(withTime);
            cal.setTimeInMillis(date);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("Parse exception");
        }
        assertEquals(2013, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.MONTH));
        assertEquals(28, cal.get(Calendar.DATE));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }
}
