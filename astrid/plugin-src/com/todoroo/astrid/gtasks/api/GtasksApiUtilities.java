package com.todoroo.astrid.gtasks.api;

import java.util.Date;
import java.util.TimeZone;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.v1.model.Task;

@SuppressWarnings("nls")
public class GtasksApiUtilities {

    public static String unixTimeToGtasksCompletionTime(long time) {
        if (time == 0) return null;
        return new DateTime(new Date(time), TimeZone.getDefault()).toStringRfc3339();
    }


    public static long gtasksCompletedTimeToUnixTime(String gtasksCompletedTime, long defaultValue) {
        if (gtasksCompletedTime == null) return defaultValue;
        try {
            long utcTime = DateTime.parseRfc3339(gtasksCompletedTime).value;
            Date date = new Date(utcTime);
            return date.getTime();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Google deals only in dates for due times, so on the server side they normalize to utc time
     * and then truncate h:m:s to 0. This can lead to a loss of date information for
     * us, so we adjust here by doing the normalizing/truncating ourselves and
     * then correcting the date we get back in a similar way.
     * @param time
     * @return
     */
    public static String unixTimeToGtasksDueDate(long time) {
        if (time == 0) return null;
        Date date = new Date(time);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        date.setTime(date.getTime() - date.getTimezoneOffset() * 60000);
        DateTime dateTime = new DateTime(date, TimeZone.getTimeZone("UTC"));
        return dateTime.toStringRfc3339();
    }

    //Adjust for google's rounding
    public static long gtasksDueTimeToUnixTime(String gtasksDueTime, long defaultValue) {
        if (gtasksDueTime == null) return defaultValue;
        try {
            long utcTime = DateTime.parseRfc3339(gtasksDueTime).value;
            Date date = new Date(utcTime);
            Date returnDate = new Date(date.getTime() + date.getTimezoneOffset() * 60000);
            return returnDate.getTime();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String extractListIdFromSelfLink(Task task) {
        String selfLink = task.selfLink;
        String [] urlComponents = selfLink.split("/");
        int listIdIndex = urlComponents.length - 3;
        return urlComponents[listIdIndex];
    }
}
