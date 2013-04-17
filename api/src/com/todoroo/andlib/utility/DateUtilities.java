/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;

import java.text.ParseException;
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
    public static String getTimeString(Context context, Date date, boolean excludeZeroMinutes) {
        String value;
        if (is24HourFormat(context)) {
            value = "H:mm";
        } else if (date.getMinutes() == 0 && excludeZeroMinutes){
            value = "h a";
        }
        else {
            value = "h:mm a";
        }
        return new SimpleDateFormat(value).format(date);
    }

    public static String getTimeString(Context context, Date date) {
        return getTimeString(context, date, true);
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
    public static String getDateString(Context context, Date date, boolean includeYear) {
        String month = DateUtils.getMonthString(date.getMonth() +
                Calendar.JANUARY, DateUtils.LENGTH_MEDIUM);
        String value;
        String standardDate;
        // united states, you are special
        Locale locale = Locale.getDefault();
        if (arrayBinaryContains(locale.getLanguage(), "ja", "ko", "zh")
                || arrayBinaryContains(locale.getCountry(),  "BZ", "CA", "KE", "MN" ,"US"))
            value = "'#' d'$'";
        else
            value = "d'$' '#'";
        if (includeYear)
            value += ", yyyy";
        if (arrayBinaryContains(locale.getLanguage(), "ja", "zh")){
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "\u65E5"); //$NON-NLS-1$
        }else if ("ko".equals(Locale.getDefault().getLanguage())){
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "\uC77C"); //$NON-NLS-1$
        }else{
            standardDate = new SimpleDateFormat(value).format(date).replace("#", month).replace("$", "");
        }
        return standardDate;}

    public static String getDateString(Context context, Date date) {
        return getDateString(context, date, true);
    }

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

    public static boolean isEndOfMonth(Date d) {
        int date = d.getDate();
        if (date < 28)
            return false;

        int month = d.getMonth();
        if (month == Calendar.FEBRUARY)
            return date >= 28;

        if (month == Calendar.APRIL || month == Calendar.JUNE || month == Calendar.SEPTEMBER || month == Calendar.NOVEMBER)
            return date >= 30;

        return date >= 31;
    }

    /**
     * Calls getRelativeDay with abbreviated parameter defaulted to true
     */
    public static String getRelativeDay(Context context, long date) {
        return DateUtilities.getRelativeDay(context, date, true);
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

    private static long clearTime(Date date) {
        date.setTime(date.getTime() / 1000L * 1000);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return date.getTime();
    }

    public static boolean isoStringHasTime(String iso8601String) {
        return iso8601String.length() > 10;
    }

    public static long parseIso8601(String iso8601String) throws ParseException {
        if (iso8601String == null)
            return 0;
        String formatString;
        if (isoStringHasTime(iso8601String)) { // Time exists
            iso8601String = iso8601String.replace("Z", "+00:00"); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                iso8601String = iso8601String.substring(0, 22) + iso8601String.substring(23);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                throw new ParseException("Invalid ISO 8601 length for string " + iso8601String, 0); //$NON-NLS-1$
            }
            formatString = "yyyy-MM-dd'T'HH:mm:ssZ"; //$NON-NLS-1$
        } else {
            formatString = "yyyy-MM-dd"; //$NON-NLS-1$
        }

        Date result = new SimpleDateFormat(formatString).parse(iso8601String);
        return result.getTime();
    }

    public static String timeToIso8601(long time, boolean includeTime) {
        if (time == 0)
            return null;
        Date date = new Date(time);
        String formatString = "yyyy-MM-dd'T'HH:mm:ssZ"; //$NON-NLS-1$
        if (!includeTime)
            formatString = "yyyy-MM-dd"; //$NON-NLS-1$
        return new SimpleDateFormat(formatString).format(date);
    }

}
