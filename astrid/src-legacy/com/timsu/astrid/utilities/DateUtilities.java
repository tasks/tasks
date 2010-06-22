/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.utilities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;

public class DateUtilities {

	private static SimpleDateFormat format = null;
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssz";

    /** Format a time into a medium length absolute format */
    public static String getFormattedDate(Context context, Date date) {
    	if(format == null)
    		format = Preferences.getDateFormat(context);
        return format.format(date);
    }

    /**
     * Convenience method for dropping the preopsition argument.
     */
    public static String getDurationString(Resources r, int timeInSeconds,
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
     * @param withPreposition whether there is a preceeding preposition
     * @return
     */
    public static String getDurationString(Resources r, int timeInSeconds,
            int unitsToShow, boolean withPreposition) {
        short days, hours, minutes, seconds;
        short unitsDisplayed = 0;

        if(timeInSeconds == 0)
            return r.getQuantityString(R.plurals.Nseconds, 0, 0);

        days = (short)(timeInSeconds / 24 / 3600);
        timeInSeconds -= days*24*3600;
        hours = (short)(timeInSeconds / 3600);
        timeInSeconds -= hours * 3600;
        minutes = (short)(timeInSeconds / 60);
        timeInSeconds -= minutes * 60;
        seconds = (short)timeInSeconds;

        StringBuilder result = new StringBuilder();
        if(days > 0) {
            int daysPlural = withPreposition ? R.plurals.NdaysPreposition : R.plurals.Ndays;
            result.append(r.getQuantityString(daysPlural, days, days)).
                append(" ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && hours > 0) {
            result.append(r.getQuantityString(R.plurals.Nhours, hours,
                    hours)).
                append(" ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow  && minutes > 0) {
            result.append(r.getQuantityString(R.plurals.Nminutes, minutes,
                    minutes)).append(" ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && seconds > 0) {
            result.append(r.getQuantityString(R.plurals.Nseconds, seconds,
                    seconds)).append(" ");
        }

        return result.toString().trim();
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
    public static String getAbbreviatedDurationString(Resources r, int timeInSeconds,
            int unitsToShow) {
        short days, hours, minutes, seconds;
        short unitsDisplayed = 0;

        if(timeInSeconds == 0)
            return r.getQuantityString(R.plurals.Nseconds, 0, 0);

        days = (short)(timeInSeconds / 24 / 3600);
        timeInSeconds -= days*24*3600;
        hours = (short)(timeInSeconds / 3600);
        timeInSeconds -= hours * 3600;
        minutes = (short)(timeInSeconds / 60);
        timeInSeconds -= minutes * 60;
        seconds = (short)timeInSeconds;

        StringBuilder result = new StringBuilder();
        if(days > 0) {
            result.append(r.getQuantityString(R.plurals.Ndays, days, days)).
                append(" ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && hours > 0) {
            result.append(r.getQuantityString(R.plurals.NhoursShort, hours,
                    hours)).
                append(" ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow  && minutes > 0) {
            result.append(r.getQuantityString(R.plurals.NminutesShort, minutes,
                    minutes)).append(" ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && seconds > 0) {
            result.append(r.getQuantityString(R.plurals.NsecondsShort, seconds,
                    seconds)).append(" ");
        }

        return result.toString().trim();
    }

    /**
     * Format a time into the format: 5 d, 3 h, 2 m
     *
     * @param r Resources to get strings from
     * @param timeInSeconds
     * @param unitsToShow number of units to show
     * @return
     */
    public static String getShortDurationString(Resources r, int timeInSeconds,
            int unitsToShow) {
        short days, hours, minutes, seconds;
        short unitsDisplayed = 0;

        if(timeInSeconds == 0)
            return "0 s";

        days = (short)(timeInSeconds / 24 / 3600);
        timeInSeconds -= days*24*3600;
        hours = (short)(timeInSeconds / 3600);
        timeInSeconds -= hours * 3600;
        minutes = (short)(timeInSeconds / 60);
        timeInSeconds -= minutes * 60;
        seconds = (short)timeInSeconds;

        StringBuilder result = new StringBuilder();
        if(days > 0) {
            result.append(days).append(" d ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && hours > 0) {
            result.append(hours).append(" h ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow  && minutes > 0) {
            result.append(minutes).append(" m ");
            unitsDisplayed++;
        }
        if(unitsDisplayed < unitsToShow && seconds > 0) {
            result.append(seconds).append(" s ");
        }

        return result.toString();
    }

    /* Format a Date into ISO 8601 Compliant format.

     */
    public static String getIso8601String(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_FORMAT);
        String result = "";
        if (d != null) {
            result = sdf.format(d);
        }
        return result;
    }

    /* Take an ISO 8601 string and return a Date object.
       On failure, returns null.
    */
    public static Date getDateFromIso8601String(String s) {
        SimpleDateFormat df = new SimpleDateFormat(ISO_8601_FORMAT);
        try {
            return df.parse(s);
        } catch (ParseException e) {
            Log.e("DateUtilities", "Error parsing ISO 8601 date");
            return null;
        }
    }

    /* Get current date and time as a string.
    Used in TasksXmlExporter
     */
    public static String getDateForExport() {
        DateFormat df = new SimpleDateFormat("yyMMdd-HHmm");
        return df.format(new Date());
    }

    public static boolean wasCreatedBefore(String s, int daysAgo) {
        DateFormat df = new SimpleDateFormat("yyMMdd");
        Date date;
        try {
            date = df.parse(s);
        } catch (ParseException e) {
            return false;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -daysAgo);
        Date calDate = cal.getTime();
        return date.before(calDate);
    }
}
