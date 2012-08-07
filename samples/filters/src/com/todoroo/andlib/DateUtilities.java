/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;


public class DateUtilities {

    /* ======================================================================
     * ============================================================ long time
     * ====================================================================== */

    /** Convert unixtime into date */
    public static final Date unixtimeToDate(long millis) {
        if(millis == 0)
            return null;
        return new Date(millis);
    }

    /** Convert date into unixtime */
    public static final long dateToUnixtime(Date date) {
        if(date == null)
            return 0;
        return date.getTime();
    }

    /** Returns unixtime for current time */
    public static final long now() {
        return System.currentTimeMillis();
    }

    /** Returns unixtime one month from now */
    public static final long oneMonthFromNow() {
        Date date = new Date();
        date.setMonth(date.getMonth() + 1);
        return date.getTime();
    }

    /** Represents a single hour */
    public static long ONE_HOUR = 3600000L;

    /** Represents a single day */
    public static long ONE_DAY = 24 * ONE_HOUR;

    /** Represents a single week */
    public static long ONE_WEEK = 7 * ONE_DAY;

    /* ======================================================================
     * =========================================================== formatters
     * ====================================================================== */

    @SuppressWarnings("nls")
    public static boolean is24HourFormat(Context context) {
        String value = android.provider.Settings.System.getString(context.getContentResolver(),
                android.provider.Settings.System.TIME_12_24);
        boolean b24 =  !(value == null || value.equals("12"));
        return b24;
    }

    /**
     * @return time format (hours and minutes)
     */
    public static SimpleDateFormat getTimeFormat(Context context) {
        String value = getTimeFormatString(context);
        return new SimpleDateFormat(value);
    }

    /**
     * @return string used for time formatting
     */
    @SuppressWarnings("nls")
    private static String getTimeFormatString(Context context) {
        String value;
        if (is24HourFormat(context)) {
            value = "H:mm";
        } else {
            value = "h:mm a";
        }
        return value;
    }

    /**
     * @return string used for date formatting
     */
    @SuppressWarnings("nls")
    private static String getDateFormatString(Context context) {
        String value = android.provider.Settings.System.getString(context.getContentResolver(),
                android.provider.Settings.System.DATE_FORMAT);
        if (value == null) {
            // united states, you are special
            if (Locale.US.equals(Locale.getDefault())
                    || Locale.CANADA.equals(Locale.getDefault()))
                value = "MMM d yyyy";
            else
                value = "d MMM yyyy";
        }
        return value;
    }

    /**
     * @return date format (month, day, year)
     */
    public static SimpleDateFormat getDateFormat(Context context) {
        return new SimpleDateFormat(getDateFormatString(context));
    }

    /**
     * @return date format as getDateFormat with weekday
     */
    @SuppressWarnings("nls")
    public static SimpleDateFormat getDateFormatWithWeekday(Context context) {
        return new SimpleDateFormat("EEE, " + getDateFormatString(context));

    }

    /**
     * @return date with time at the end
     */
    @SuppressWarnings("nls")
    public static SimpleDateFormat getDateWithTimeFormat(Context context) {
        return new SimpleDateFormat(getDateFormatString(context) + " " +
                getTimeFormatString(context));

    }

    /**
     * @return formatted date (will contain month, day, year)
     */
    public static String getFormattedDate(Context context, Date date) {
        return getDateFormat(context).format(date);
    }

}
