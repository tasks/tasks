/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.Task;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

public class GtasksApiUtilities {
    public static String LINK_TYPE = "astrid";
    public static String HIDE_UNTIL = "Hide until";
    public static String ASTRID_URL = "https://github.com/tasks/tasks";

    public static DateTime unixTimeToGtasksCompletionTime(long time) {
        if (time < 0) {
            return null;
        }
        return new DateTime(new Date(time), TimeZone.getDefault());
    }

    public static long gtasksCompletedTimeToUnixTime(DateTime gtasksCompletedTime) {
        if (gtasksCompletedTime == null) {
            return 0;
        }
        return gtasksCompletedTime.getValue();
    }

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
        Date date = new Date(time / 1000 * 1000);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        date.setTime(date.getTime() - date.getTimezoneOffset() * 60000);
        return new DateTime(date, TimeZone.getTimeZone("GMT"));
    }

    //Adjust for google's rounding
    public static long gtasksDueTimeToUnixTime(DateTime gtasksDueTime) {
        if (gtasksDueTime == null) {
            return 0;
        }
        try {
            long utcTime = gtasksDueTime.getValue(); //DateTime.parseRfc3339(gtasksDueTime).value;
            Date date = new Date(utcTime);
            Date returnDate = new Date(date.getTime() + date.getTimezoneOffset() * 60000);
            return returnDate.getTime();
        } catch (NumberFormatException e) {
            Timber.e(e, e.getMessage());
            return 0;
        }
    }

    /**
     * @param hideUntilUnixtime Hide until this timestamp
     */
    public static void addHideUntilTime(Task gtask, Long hideUntilUnixtime) {
        if (hideUntilUnixtime == null) {
            return;
        }

        List<Task.Links> links = gtask.getLinks();
        if (links == null) {
            links = new LinkedList<>();
            gtask.setLinks(links);
        }

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        String dateString = df.format(new Date(hideUntilUnixtime * 1000));

        Task.Links taskLink = new Task.Links();
        taskLink.setType(LINK_TYPE);
        taskLink.setLink(ASTRID_URL);
        taskLink.setDescription(HIDE_UNTIL + ": " + dateString);

        links.add(taskLink);
    }

    /**
     * Parse the links section of a GTasks object and update our local task with what we find there.
     */
    public static void parseLinks(List<Task.Links> links, com.todoroo.astrid.data.Task task) {
        if (links == null) {
            return;
        }

        for (Task.Links link: links) {
            parseHideUntilLink(link, task);
        }
    }

    private static void parseHideUntilLink(Task.Links link, com.todoroo.astrid.data.Task task) {
        if (!LINK_TYPE.equals(link.getType())) {
            return;
        }

        String description = link.getDescription();
        if (description == null) {
            return;
        }
        if (!description.startsWith(HIDE_UNTIL + ": ")) {
            return;
        }

        String dateString = description.substring((HIDE_UNTIL + ": ").length());
        Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH).parse(dateString);
        } catch (ParseException e) {
            Timber.w(e, "Failed to parse synced date");
            return;
        }
        long javatime = date.getTime();
        long unixtime = javatime / 1000;

        task.setHideUntil(unixtime);
    }
}
