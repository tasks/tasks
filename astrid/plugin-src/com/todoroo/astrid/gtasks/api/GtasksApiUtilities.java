package com.todoroo.astrid.gtasks.api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.v1.model.Task;

@SuppressWarnings("nls")
public class GtasksApiUtilities {

    private static SimpleDateFormat timeWriter = new SimpleDateFormat("yyyy-MM-dd'T'hh:m:ss.SSSZ", Locale.US);

    //When setting completion date, gtasks api will convert to UTC AND change hours/minutes/seconds to match
    public static long gtasksCompletedTimeToUnixTime(String gtasksCompletedTime, long defaultValue) {
        if (gtasksCompletedTime == null) return defaultValue;
        synchronized(timeWriter) {
            try {
                long utcTime = DateTime.parseRfc3339(gtasksCompletedTime).value;
                return new DateTime(new Date(utcTime), TimeZone.getDefault()).value;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    //When setting due date, gtasks api will convert to UTC time without changing hours/minutes/seconds
    public static long gtasksDueTimeToUnixTime(String gtasksDueTime, long defaultValue) {
        if (gtasksDueTime == null) return defaultValue;
        synchronized(timeWriter) {
            try {
                long utcTime = DateTime.parseRfc3339(gtasksDueTime).value;
                return utcTime - TimeZone.getDefault().getOffset(utcTime);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    public static String unixTimeToGtasksTime(long time) {
        if (time == 0) return null;
        synchronized(timeWriter) {
            return new DateTime(new Date(time), TimeZone.getDefault()).toStringRfc3339();
        }
    }

    public static String unixTimeToGtasksDate(long time) {
        if (time == 0) return null;
        synchronized(timeWriter) {
            Date date = new Date(time);
            date.setHours(0);
            date.setMinutes(0);
            date.setSeconds(0);
            return new DateTime(date, TimeZone.getDefault()).toStringRfc3339();
        }
    }

    /*
     * The two methods below are useful for testing
     */
    public static String gtasksCompletedTimeStringToLocalTimeString(String gtasksTime) {
        return GtasksApiUtilities.unixTimeToGtasksTime(GtasksApiUtilities.gtasksCompletedTimeToUnixTime(gtasksTime, 0));
    }

    public static String gtasksDueTimeStringToLocalTimeString(String gtasksTime) {
        return GtasksApiUtilities.unixTimeToGtasksTime(GtasksApiUtilities.gtasksDueTimeToUnixTime(gtasksTime, 0));
    }

    public static String extractListIdFromSelfLink(Task task) {
        String selfLink = task.selfLink;
        String [] urlComponents = selfLink.split("/");
        int listIdIndex = urlComponents.length - 3;
        return urlComponents[listIdIndex];
    }
}
