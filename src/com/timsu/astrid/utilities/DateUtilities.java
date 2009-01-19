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

import android.content.res.Resources;

import com.timsu.astrid.R;

public class DateUtilities {

    /**
     * Format a time into the format: 5 days, 3 hours, 2 minutes
     *
     * @param r Resources to get strings from
     * @param timeInSeconds
     * @param unitsToShow number of units to show (i.e. if 2, then 5 hours
     *        3 minutes 2 seconds is truncated to 5 hours 3 minutes)
     * @return
     */
    public static String getDurationString(Resources r, int timeInSeconds,
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
}
