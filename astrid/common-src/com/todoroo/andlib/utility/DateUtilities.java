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
    public static long ONE_DAY = 3600000L;

    /** Represents a single week */
    public static long ONE_WEEK = 7 * 3600000L;

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
        int years, months, days, hours, minutes, seconds;
        short unitsDisplayed = 0;
        duration = Math.abs(duration);

        if(duration == 0)
            return r.getQuantityString(secondsResource, 0, 0);

        Date now = new Date();
        Date then = new Date(DateUtilities.now() + duration);

        years = then.getYear() - now.getYear();
        months = then.getMonth() - now.getMonth();
        days = then.getDate() - now.getDate();
        hours = then.getHours() - now.getHours();
        minutes = then.getMinutes() - now.getMinutes();
        seconds = then.getSeconds() - now.getSeconds();

        StringBuilder result = new StringBuilder();
        unitsDisplayed = displayUnits(r, yearsResource, unitsToShow, years, months >= 6,
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, monthsResource, unitsToShow, months, days >= 15,
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, daysResource, unitsToShow, days, hours >= 12,
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, hoursResource, unitsToShow, hours, minutes >= 30,
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, minutesResource, unitsToShow, minutes, seconds >= 30,
                unitsDisplayed, result);
        unitsDisplayed = displayUnits(r, secondsResource, unitsToShow, seconds, false,
                unitsDisplayed, result);

        return result.toString().trim();
    }

    /** Display units, rounding up if necessary. Returns units to show */
    private short displayUnits(Resources r, int resource, int unitsToShow, int value,
            boolean shouldRound, short unitsDisplayed, StringBuilder result) {
        if(unitsDisplayed < unitsToShow && value > 0) {
            // round up if needed
            if(unitsDisplayed + 1 == unitsToShow && shouldRound)
                value++;
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
