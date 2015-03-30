/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import android.content.Context;
import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.tasks.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.tasks.date.DateTimeUtils.currentTimeMillis;
import static org.tasks.date.DateTimeUtils.newDate;


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

    /**
     * @param context android context
     * @param date time to format
     * @return time, with hours and minutes
     */
    public static String getTimeString(Context context, Date date) {
        String value;
        if (is24HourFormat(context)) {
            value = "HH:mm";
        } else if (date.getMinutes() == 0){
            value = "h a";
        } else {
            value = "h:mm a";
        }
        return new SimpleDateFormat(value).format(date);
    }

    /* Returns true if search string is in sortedValues */

    private static boolean arrayBinaryContains(String search, String... sortedValues) {
        return Arrays.binarySearch(sortedValues, search) >= 0;
    }


    public static String getLongDateString(Date date) {
        return getDateString(new SimpleDateFormat("MMMM"), date);
    }

    /**
     * @param date date to format
     * @return date, with month, day, and year
     */
    public static String getDateString(Date date) {
        return getDateString(new SimpleDateFormat("MMM"), date);
    }

    private static String getDateString(SimpleDateFormat simpleDateFormat, Date date) {
        String month = simpleDateFormat.format(date);
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
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "\u65E5"); //$NON-NLS-1$
        }else if ("ko".equals(Locale.getDefault().getLanguage())){
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "\uC77C"); //$NON-NLS-1$
        }else{
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "");
        }
        return standardDate;
    }

    public static String getLongDateStringHideYear(Date date) {
        return getDateStringHideYear(new SimpleDateFormat("MMMM"), date);
    }

    /**
     * @param date date to format
     * @return date, with month, day, and year
     */
    public static String getDateStringHideYear(Date date) {
        return getDateStringHideYear(new SimpleDateFormat("MMM"), date);
    }

    private static String getDateStringHideYear(SimpleDateFormat simpleDateFormat, Date date) {
        String month = simpleDateFormat.format(date);
        String value;
        Locale locale = Locale.getDefault();
        if (arrayBinaryContains(locale.getLanguage(), "ja", "ko", "zh")
                || arrayBinaryContains(locale.getCountry(),  "BZ", "CA", "KE", "MN" ,"US")) {
            value = "'#' d";
        } else {
            value = "d '#'";
        }

        if (date.getYear() !=  (newDate()).getYear()) {
            value = value + "\nyyyy";
        }
        if (arrayBinaryContains(locale.getLanguage(), "ja", "zh")) //$NON-NLS-1$
        {
            return new SimpleDateFormat(value).format(date).replace("#", month) + "\u65E5"; //$NON-NLS-1$
        } else if ("ko".equals(Locale.getDefault().getLanguage())) //$NON-NLS-1$
        {
            return new SimpleDateFormat(value).format(date).replace("#", month) + "\uC77C"; //$NON-NLS-1$
        } else {
            return new SimpleDateFormat(value).format(date).replace("#", month);
        }
    }

    /**
     * @return weekday
     */
    public static String getWeekday(Date date) {
        return new SimpleDateFormat("EEEE").format(date);
    }

    /**
     * @return weekday
     */
    public static String getWeekdayShort(Date date) {
        return new SimpleDateFormat("EEE").format(date);
    }

    /**
     * @return date with time at the end
     */
    public static String getDateStringWithTime(Context context, Date date) {
        return getDateString(date) + " " + getTimeString(context, date);
    }

    public static String getRelativeDay(Context context, long date) {
        return DateUtilities.getRelativeDay(context, date, true);
    }

    /**
     * @return yesterday, today, tomorrow, or null
     */
    public static String getRelativeDay(Context context, long date, boolean abbreviated) {
        long today = clearTime(newDate());
        long input = clearTime(newDate(date));

        if(today == input) {
            return context.getString(R.string.today).toLowerCase();
        }

        if(today + ONE_DAY == input) {
            return context.getString(abbreviated ? R.string.tmrw : R.string.tomorrow).toLowerCase();
        }

        if(today == input + ONE_DAY) {
            return context.getString(abbreviated ? R.string.yest : R.string.yesterday).toLowerCase();
        }

        if(today + abbreviationLimit >= input && today - abbreviationLimit <= input) {
            return abbreviated ? DateUtilities.getWeekdayShort(newDate(date)) : DateUtilities.getWeekday(newDate(date));
        }

        return DateUtilities.getDateStringHideYear(newDate(date));
    }

    public static boolean isEndOfMonth(Date d) {
        return d.getDate() == new DateTime(d).dayOfMonth().getMaximumValue();
    }

    private static final Calendar calendar = Calendar.getInstance();
    public static long getStartOfDay(long time) {
        calendar.setTimeInMillis(time);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    static long clearTime(Date date) {
        date.setTime(date.getTime() / 1000L * 1000);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return date.getTime();
    }
}
