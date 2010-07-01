/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.andlib.utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;


public class DateUtilities {

    @Autowired
    public Integer yearsResource;

    @Autowired
    public Integer monthsResource;

    @Autowired
    public Integer weeksResource;

    @Autowired
    public Integer daysResource;

    @Autowired
    public Integer hoursResource;

    @Autowired
    public Integer minutesResource;

    @Autowired
    public Integer secondsResource;

    @Autowired
    public Integer daysAbbrevResource;

    @Autowired
    public Integer hoursAbbrevResource;

    @Autowired
    public Integer minutesAbbrevResource;

    @Autowired
    public Integer secondsAbbrevResource;

    public DateUtilities() {
        DependencyInjectionService.getInstance().inject(this);
    }

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

    /** Represents a single day */
    public static long ONE_DAY = 24 * 3600000L;

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


    /* ======================================================================
     * ============================================================= duration
     * ====================================================================== */

    /**
     * Convenience method for dropping the preposition argument.
     * @param duration in millis
     * @param unitsToShow number of units to show (i.e. if 2, then 5 hours
     *        3 minutes 2 seconds is truncated to 5 hours 3 minutes)
     */
    public String getDurationString(long duration, int unitsToShow) {
        return getDurationString(duration, unitsToShow, false);
    }

    /**
     * Format a time into the format: 5 days, 3 hours, 2 minutes
     *
     * @param duration in millis
     * @param unitsToShow number of units to show (i.e. if 2, then 5 hours
     *        3 minutes 2 seconds is truncated to 5 hours 3 minutes)
     * @param withPreposition whether there is a preceding preposition
     * @return
     */
    public String getDurationString(long duration, int unitsToShow, boolean withPreposition) {
        Resources r = ContextManager.getContext().getResources();
        short unitsDisplayed = 0;
        duration = Math.abs(duration);

        if(duration == 0)
            return r.getQuantityString(secondsResource, 0, 0);

        Date now = new Date(80, 1, 1);
        Date then = new Date(now.getTime() + duration);

        int[] values = new int[] {
            then.getYear() - now.getYear(),
            then.getMonth() - now.getMonth(),
            (then.getDate() - now.getDate())/7,
            (then.getDate() - now.getDate()) - (then.getDate() - now.getDate())/7*7,
            then.getHours() - now.getHours(),
            then.getMinutes() - now.getMinutes(),
            then.getSeconds() - now.getSeconds(),
        };
        int[] maxValues = new int[] {
                Integer.MAX_VALUE,
                12,
                5,
                7,
                24,
                60,
                60
        };

        // perform rounding (this is definitely magic... trust the unit tests)
        int cursor = 0;
        while(values[cursor] == 0 && ++cursor < values.length)
            ;
        int postCursor = cursor + unitsToShow;
        for(int i = values.length - 1; i >= postCursor; i--) {
            if(values[i] >= maxValues[i]/2) {
                values[i-1]++;
            }
        }
        for(int i = Math.min(values.length, postCursor) - 1; i >= 1; i--) {
            if(values[i] == maxValues[i]) {
                values[i-1]++;
                for(int j = i; j < values.length; j++)
                    values[j] = 0;
            }
        }


        StringBuilder result = new StringBuilder();
        unitsDisplayed = displayUnits(r, yearsResource, unitsToShow, values[0],
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, monthsResource, unitsToShow, values[1],
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, weeksResource, unitsToShow, values[2],
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, daysResource, unitsToShow, values[3],
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, hoursResource, unitsToShow, values[4],
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, minutesResource, unitsToShow, values[5],
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, secondsResource, unitsToShow, values[6],
                unitsDisplayed, result);

        return result.toString().trim();
    }

    /** Display units, rounding up if necessary. Returns units to show */
    private short displayUnits(Resources r, int resource, int unitsToShow, int value,
            short unitsDisplayed, StringBuilder result) {
        if(unitsDisplayed < unitsToShow && value > 0) {
            result.append(r.getQuantityString(resource, value, value)).
            append(' ');
            unitsDisplayed++;
        }
        return unitsDisplayed;
    }

    /**
     * Format a time into the format: 5 days, 3 hrs, 2 min
     *
     * @param r Resources to get strings from
     * @param timeInSeconds
     * @param unitsToShow number of units to show (i.e. if 2, then 5 hours
     *        3 minutes 2 seconds is truncated to 5 hours 3 minutes)
     * @return
     */
    public String getAbbreviatedDurationString(Resources r, int timeInSeconds,
            int unitsToShow) {
        short days, hours, minutes, seconds;
        short unitsDisplayed = 0;
        timeInSeconds = Math.abs(timeInSeconds);

        if(timeInSeconds == 0)
            return r.getQuantityString(secondsAbbrevResource, 0, 0);

        days = (short)(timeInSeconds / 24 / 3600);
        timeInSeconds -= days*24*3600;
        hours = (short)(timeInSeconds / 3600);
        timeInSeconds -= hours * 3600;
        minutes = (short)(timeInSeconds / 60);
        timeInSeconds -= minutes * 60;
        seconds = (short)timeInSeconds;

        StringBuilder result = new StringBuilder();
        if(days > 0) {
            // round up if needed
            if(unitsDisplayed == unitsToShow && hours >= 12)
                days++;
            result.append(r.getQuantityString(daysAbbrevResource, days, days)).
            append(' ');
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && hours > 0) {
            // round up if needed
            if(unitsDisplayed == unitsToShow && minutes >= 30)
                days++;
            result.append(r.getQuantityString(hoursAbbrevResource, hours,
                    hours)).
                append(' ');
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow  && minutes > 0) {
            // round up if needed
            if(unitsDisplayed == unitsToShow && seconds >= 30)
                days++;
            result.append(r.getQuantityString(minutesAbbrevResource, minutes,
                    minutes)).append(' ');
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && seconds > 0) {
            result.append(r.getQuantityString(secondsAbbrevResource, seconds,
                    seconds)).append(' ');
        }

        return result.toString().trim();
    }

}
