/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import com.google.api.client.util.DateTime;

import java.util.Date;
import java.util.TimeZone;

import static org.tasks.date.DateTimeUtils.newDate;

public class GtasksApiUtilities {

    public static DateTime unixTimeToGtasksCompletionTime(long time) {
        if (time < 0) {
            return null;
        }
        return new DateTime(newDate(time), TimeZone.getDefault());
    }

//    public static String unixTimeToGtasksCompletionTime(long time) {
//        if (time == 0) return null;
//        return new DateTime(newDate(time), TimeZone.getDefault()).toStringRfc3339();
//    }


    public static long gtasksCompletedTimeToUnixTime(DateTime gtasksCompletedTime, long defaultValue) {
        if (gtasksCompletedTime == null) {
            return defaultValue;
        }
        return gtasksCompletedTime.getValue();
    }

//    public static long gtasksCompletedTimeToUnixTime(String gtasksCompletedTime, long defaultValue) {
//        if (gtasksCompletedTime == null) return defaultValue;
//        try {
//            long utcTime = DateTime.parseRfc3339(gtasksCompletedTime).value;
//            Date date = newDate(utcTime);
//            return date.getTime();
//        } catch (NumberFormatException e) {
//            return defaultValue;
//        }
//    }

    /**
     * Google deals only in dates for due times, so on the server side they normalize to utc time
     * and then truncate h:m:s to 0. This can lead to a loss of date information for
     * us, so we adjust here by doing the normalizing/truncating ourselves and
     * then correcting the date we get back in a similar way.
     */
    public static DateTime unixTimeToGtasksDueDate(long time) {
        if (time < 0) {
            return null;
        }
        Date date = newDate(time);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        date.setTime(date.getTime() - date.getTimezoneOffset() * 60000);
        return new DateTime(date, TimeZone.getTimeZone("UTC"));
    }

//    public static DateTime unixTimeToGtasksDueDate(long time) {
//        if (time == 0) return null;
//        return new DateTime(time, 0);
//    }

    //Adjust for google's rounding
    public static long gtasksDueTimeToUnixTime(DateTime gtasksDueTime, long defaultValue) {
        if (gtasksDueTime == null) {
            return defaultValue;
        }
        try {
            long utcTime = gtasksDueTime.getValue(); //DateTime.parseRfc3339(gtasksDueTime).value;
            Date date = newDate(utcTime);
            Date returnDate = newDate(date.getTime() + date.getTimezoneOffset() * 60000);
            return returnDate.getTime();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

//    public static long gtasksDueTimeToUnixTime(DateTime gtasksDueTime, long defaultValue) {
//        if (gtasksDueTime == null) return defaultValue;
//        return gtasksDueTime.getValue();
//    }

}
