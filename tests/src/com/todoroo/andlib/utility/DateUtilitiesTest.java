/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import java.util.Date;

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
}
