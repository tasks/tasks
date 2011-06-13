/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.andlib.utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import com.todoroo.astrid.api.R;


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

    static Boolean is24HourOverride = null;

    public static boolean is24HourFormat(Context context) {
        if(is24HourOverride != null)
            return is24HourOverride;

        return DateFormat.is24HourFormat(context);
    }

    /**
     * @param context android context
     * @param date time to format
     * @return time, with hours and minutes
     */
    @SuppressWarnings("nls")
    public static String getTimeString(Context context, Date date) {
        String value;
        if (is24HourFormat(context)) {
            value = "H:mm";
        } else {
            value = "h:mm a";
        }
        return new SimpleDateFormat(value).format(date);
    }

    /**
     * @param context android context
     * @param date date to format
     * @return date, with month, day, and year
     */
    @SuppressWarnings("nls")
    public static String getDateString(Context context, Date date) {
        String month = DateUtils.getMonthString(date.getMonth() +
                Calendar.JANUARY, DateUtils.LENGTH_MEDIUM);
        String value;
        // united states, you are special
        if (Locale.US.equals(Locale.getDefault())
                || Locale.CANADA.equals(Locale.getDefault()))
            value = "'#' d yyyy";
        else
            value = "d '#' yyyy";
        return new SimpleDateFormat(value).format(date).replace("#", month);
    }

    /**
     * @return date format as getDateFormat with weekday
     */
    @SuppressWarnings("nls")
    public static String getDateStringWithWeekday(Context context, Date date) {
        String weekday = DateUtils.getDayOfWeekString(date.getDay() + Calendar.SUNDAY,
                DateUtils.LENGTH_LONG);
        return weekday + ", " + getDateString(context, date);
    }

    /**
     * @return date format as getDateFormat with weekday
     */
    @SuppressWarnings("nls")
    public static String getDateStringWithTimeAndWeekday(Context context, Date date) {
        return getDateStringWithWeekday(context, date) + " " + getTimeString(context, date);
    }

    /**
     * @return date with time at the end
     */
    @SuppressWarnings("nls")
    public static String getDateStringWithTime(Context context, Date date) {
        return getDateString(context, date) + " " + getTimeString(context, date);
    }

    /**
     * @return yesterday, today, tomorrow, or null
     */
    public static String getRelativeDay(Context context, long date) {
        Date today = new Date();
        if(Math.abs(today.getTime() - date) > DateUtilities.ONE_DAY)
            return null;
        int todayDate = today.getDate();
        int otherDate = unixtimeToDate(date).getDate();

        if(todayDate == otherDate)
            return context.getString(R.string.today);
        if(today.getTime() > date)
            return context.getString(R.string.yesterday);
        return context.getString(R.string.tomorrow);

    }

}
