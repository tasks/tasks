/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import java.text.SimpleDateFormat;
import java.util.Arrays;
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

    /**
     * Add the specified amount of months to the given time.<br/>
     * The day of month will stay the same.<br/>
     *
     * @param time the base-time (in milliseconds) to which the amount of months is added
     * @param interval the amount of months to be added
     * @return the calculated time in milliseconds
     */
    public static final long addCalendarMonthsToUnixtime(long time, int interval) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.add(Calendar.MONTH, interval);
        return c.getTimeInMillis();
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

    /** Represents a single minute */
    public static long ONE_MINUTE = 60000L;

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
        } else if (date.getMinutes() == 0){
            value = "h a";
        }
        else {
            value = "h:mm a";
        }
        return new SimpleDateFormat(value).format(date);
    }

    /* Returns true if search string is in sortedValues */

    private static boolean arrayBinaryContains(String search, String... sortedValues) {
        return Arrays.binarySearch(sortedValues, search) >= 0;
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
        String standardDate;
        // united states, you are special
        Locale locale = Locale.getDefault();
        if (arrayBinaryContains(locale.getLanguage(), "ja", "ko", "zh")
                || arrayBinaryContains(locale.getCountry(),  "BZ", "CA", "KE", "MN" ,"US"))
            value = "'#' d'$' yyyy";
        else
            value = "d'$' '#' yyyy";
        if (arrayBinaryContains(locale.getLanguage(), "ja", "zh")){
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "\u65E5"); //$NON-NLS-1$
        }else if ("ko".equals(Locale.getDefault().getLanguage())){
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "\uC77C"); //$NON-NLS-1$
        }else{
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "");
        }
        return standardDate;}

    /**
     * @param context android context
     * @param date date to format
     * @return date, with month, day, and year
     */
    @SuppressWarnings("nls")
    public static String getDateStringHideYear(Context context, Date date) {
        String month = DateUtils.getMonthString(date.getMonth() +
                Calendar.JANUARY, DateUtils.LENGTH_MEDIUM);
        String value;
        Locale locale = Locale.getDefault();
        // united states, you are special
        if (arrayBinaryContains(locale.getLanguage(), "ja", "ko", "zh")
                || arrayBinaryContains(locale.getCountry(),  "BZ", "CA", "KE", "MN" ,"US"))
            value = "'#' d";
        else
            value = "d '#'";

        if (date.getYear() !=  (new Date()).getYear()) {
            value = value + "\nyyyy";
        }
        if (arrayBinaryContains(locale.getLanguage(), "ja", "zh")) //$NON-NLS-1$
            return new SimpleDateFormat(value).format(date).replace("#", month) + "\u65E5"; //$NON-NLS-1$
        else if ("ko".equals(Locale.getDefault().getLanguage())) //$NON-NLS-1$
            return new SimpleDateFormat(value).format(date).replace("#", month) + "\uC77C"; //$NON-NLS-1$
        else
            return new SimpleDateFormat(value).format(date).replace("#", month);
    }

    /**
     * @return date format as getDateFormat with weekday
     */
    @SuppressWarnings("nls")
    public static String getDateStringWithWeekday(Context context, Date date) {
        String weekday = getWeekday(date);
        return weekday + ", " + getDateString(context, date);
    }

    /**
     * @return weekday
     */
    public static String getWeekday(Date date) {
        return DateUtils.getDayOfWeekString(date.getDay() + Calendar.SUNDAY,
                DateUtils.LENGTH_LONG);
    }


    /**
     * @return weekday
     */
    public static String getWeekdayShort(Date date) {
        return DateUtils.getDayOfWeekString(date.getDay() + Calendar.SUNDAY,
                DateUtils.LENGTH_MEDIUM);
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
    public static String getRelativeDay(Context context, long date, boolean abbreviated) {
        long today = clearTime(new Date());
        long input = clearTime(new Date(date));

        if(today == input)
            return context.getString(R.string.today).toLowerCase();

        if(today + ONE_DAY == input)
            return context.getString(abbreviated ? R.string.tmrw : R.string.tomorrow).toLowerCase();

        if(today == input + ONE_DAY)
            return context.getString(abbreviated ? R.string.yest : R.string.yesterday).toLowerCase();

        if(today + DateUtilities.ONE_WEEK >= input &&
                today - DateUtilities.ONE_WEEK <= input)
            return abbreviated ? DateUtilities.getWeekdayShort(new Date(date)) : DateUtilities.getWeekday(new Date(date));

        return DateUtilities.getDateStringHideYear(context, new Date(date));
    }

    /**
     * Calls getRelativeDay with abbreviated parameter defaulted to true
     */
    public static String getRelativeDay(Context context, long date) {
        return DateUtilities.getRelativeDay(context, date, true);
    }

    public static long getStartOfDay(long time) {
        Date date = new Date(time);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return date.getTime();
    }

    public static long getEndOfDay(long time) {
        Date date = new Date(time);
        date.setHours(23);
        date.setMinutes(59);
        date.setSeconds(59);
        return date.getTime();
    }

    private static long clearTime(Date date) {
        date.setTime(date.getTime() / 1000L * 1000);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return date.getTime();
    }

}
