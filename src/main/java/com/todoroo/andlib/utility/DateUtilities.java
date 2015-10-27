/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import android.content.Context;
import android.text.format.DateFormat;

import org.tasks.R;
import org.tasks.time.DateTime;

import java.util.Arrays;
import java.util.Locale;

import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;


public class DateUtilities {

    private static final long abbreviationLimit = DateUtilities.ONE_DAY  * 6;

    /**
     * Add the specified amount of months to the given time.<br/>
     * The day of month will stay the same.<br/>
     *
     * @param time the base-time (in milliseconds) to which the amount of months is added
     * @param interval the amount of months to be added
     * @return the calculated time in milliseconds
     */
    public static long addCalendarMonthsToUnixtime(long time, int interval) {
        DateTime dt = new DateTime(time);
        DateTime result = dt.plusMonths(interval);
        // preserving java.util.date behavior
        int diff = dt.getDayOfMonth() - result.getDayOfMonth();
        if(diff > 0) {
            result = result.plusDays(diff);
        }
        return result.getMillis();
    }

    /** Returns unixtime for current time */
    public static long now() {
        return currentTimeMillis();
    }

    /** Returns unixtime one month from now */
    public static long oneMonthFromNow() {
        return addCalendarMonthsToUnixtime(currentTimeMillis(), 1);
    }

    /** Represents a single hour */
    public static final long ONE_HOUR = 3600000L;

    /** Represents a single day */
    public static final long ONE_DAY = 24 * ONE_HOUR;

    /** Represents a single week */
    public static final long ONE_WEEK = 7 * ONE_DAY;

    /** Represents a single minute */
    public static final long ONE_MINUTE = 60000L;

    /* ======================================================================
     * =========================================================== formatters
     * ====================================================================== */

    static Boolean is24HourOverride = null;

    public static boolean is24HourFormat(Context context) {
        if(is24HourOverride != null) {
            return is24HourOverride;
        }

        return DateFormat.is24HourFormat(context);
    }

    public static String getTimeString(Context context, long timestamp) {
        return getTimeString(context, newDateTime(timestamp));
    }

    public static String getTimeString(Context context, DateTime date) {
        String value;
        if (is24HourFormat(context)) {
            value = "HH:mm";
        } else if (date.getMinuteOfHour() == 0){
            value = "h a";
        } else {
            value = "h:mm a";
        }
        return date.toString(value);
    }

    /* Returns true if search string is in sortedValues */

    private static boolean arrayBinaryContains(String search, String... sortedValues) {
        return Arrays.binarySearch(sortedValues, search) >= 0;
    }


    public static String getLongDateString(DateTime date) {
        return getDateString("MMMM", date);
    }

    /**
     * @param date date to format
     * @return date, with month, day, and year
     */
    public static String getDateString(DateTime date) {
        return getDateString("MMM", date);
    }

    private static String getDateString(String simpleDateFormat, DateTime date) {
        String month = date.toString(simpleDateFormat);
        String value;
        String standardDate;
        Locale locale = Locale.getDefault();
        if (arrayBinaryContains(locale.getLanguage(), "ja", "ko", "zh")
                || arrayBinaryContains(locale.getCountry(),  "BZ", "CA", "KE", "MN" ,"US")) {
            value = "'#' d'$'";
        } else {
            value = "d'$' '#'";
        }
        value += ", yyyy";
        if (arrayBinaryContains(locale.getLanguage(), "ja", "zh")){
            standardDate = date.toString(value).replace("#", month).replace("$", "\u65E5"); //$NON-NLS-1$
        }else if ("ko".equals(Locale.getDefault().getLanguage())){
            standardDate = date.toString(value).replace("#", month).replace("$", "\uC77C"); //$NON-NLS-1$
        }else{
            standardDate = date.toString(value).replace("#", month).replace("$", "");
        }
        return standardDate;
    }

    public static String getLongDateStringHideYear(DateTime date) {
        return getDateStringHideYear("MMMM", date);
    }

    /**
     * @param date date to format
     * @return date, with month, day, and year
     */
    public static String getDateStringHideYear(DateTime date) {
        return getDateStringHideYear("MMM", date);
    }

    private static String getDateStringHideYear(String simpleDateFormat, DateTime date) {
        String month = date.toString(simpleDateFormat);
        String value;
        Locale locale = Locale.getDefault();
        if (arrayBinaryContains(locale.getLanguage(), "ja", "ko", "zh")
                || arrayBinaryContains(locale.getCountry(),  "BZ", "CA", "KE", "MN" ,"US")) {
            value = "'#' d";
        } else {
            value = "d '#'";
        }

        if (date.getYear() !=  (newDateTime()).getYear()) {
            value = value + "\nyyyy";
        }
        if (arrayBinaryContains(locale.getLanguage(), "ja", "zh")) //$NON-NLS-1$
        {
            return date.toString(value).replace("#", month) + "\u65E5"; //$NON-NLS-1$
        } else if ("ko".equals(Locale.getDefault().getLanguage())) //$NON-NLS-1$
        {
            return date.toString(value).replace("#", month) + "\uC77C"; //$NON-NLS-1$
        } else {
            return date.toString(value).replace("#", month);
        }
    }

    /**
     * @return weekday
     */
    public static String getWeekday(DateTime date) {
        return date.toString("EEEE");
    }

    /**
     * @return weekday
     */
    public static String getWeekdayShort(DateTime date) {
        return date.toString("EEE");
    }

    public static String getDateStringWithTime(Context context, long timestamp) {
        return getDateStringWithTime(context, newDateTime(timestamp));
    }

    public static String getDateStringWithTime(Context context, DateTime date) {
        return getDateString(date) + " " + getTimeString(context, date);
    }

    /**
     * @return yesterday, today, tomorrow, or null
     */
    public static String getRelativeDay(Context context, long date, boolean abbreviated) {
        long today = getStartOfDay(currentTimeMillis());
        long input = getStartOfDay(date);

        if(today == input) {
            return context.getString(R.string.today);
        }

        if(today + ONE_DAY == input) {
            return context.getString(abbreviated ? R.string.tmrw : R.string.tomorrow);
        }

        if(today == input + ONE_DAY) {
            return context.getString(abbreviated ? R.string.yest : R.string.yesterday);
        }

        if(today + abbreviationLimit >= input && today - abbreviationLimit <= input) {
            return abbreviated ? DateUtilities.getWeekdayShort(newDateTime(date)) : DateUtilities.getWeekday(newDateTime(date));
        }

        return DateUtilities.getDateStringHideYear(newDateTime(date));
    }

    public static long getStartOfDay(long time) {
        return newDateTime(time).startOfDay().getMillis();
    }
}
