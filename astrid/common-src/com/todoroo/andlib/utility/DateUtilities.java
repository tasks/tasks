/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.andlib.utility;

import java.util.Date;

import android.content.res.Resources;

import com.todoroo.andlib.service.Autowired;
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
     * ============================================================ unix time
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
    public static long WEEK = 7 * 3600000L;

    /* ======================================================================
     * =========================================================== formatters
     * ====================================================================== */

    /**
     * Convenience method for dropping the preposition argument.
     */
    public String getDurationString(Resources r, int timeInSeconds,
            int unitsToShow) {
        return getDurationString(r, timeInSeconds, unitsToShow, false);
    }

    /**
     * Format a time into the format: 5 days, 3 hours, 2 minutes
     *
     * @param r Resources to get strings from
     * @param timeInSeconds
     * @param unitsToShow number of units to show (i.e. if 2, then 5 hours
     *        3 minutes 2 seconds is truncated to 5 hours 3 minutes)
     * @param withPreposition whether there is a preceding preposition
     * @return
     */
    public String getDurationString(Resources r, int timeInSeconds,
            int unitsToShow, boolean withPreposition) {
        int years, months, days, hours, minutes, seconds;
        short unitsDisplayed = 0;
        timeInSeconds = Math.abs(timeInSeconds);

        if(timeInSeconds == 0)
            return r.getQuantityString(secondsResource, 0, 0);

        Date now = new Date(80, 0, 1);
        Date then = unixtimeToDate((int)(now.getTime() / 1000L) + timeInSeconds);

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
