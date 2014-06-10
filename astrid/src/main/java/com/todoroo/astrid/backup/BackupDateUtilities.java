/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Date Utility functions for backups
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class BackupDateUtilities {

    private static final Logger log = LoggerFactory.getLogger(BackupDateUtilities.class);

    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssz";

    /**
     * Take an ISO 8601 string and return a Date object.
     * On failure, returns null.
     */
    public static Date getDateFromIso8601String(String s) {
        SimpleDateFormat df = new SimpleDateFormat(ISO_8601_FORMAT);
        try {
            return df.parse(s);
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static Date getTaskDueDateFromIso8601String(String s) {
        System.err.println("Importing date string: " + s);
        Date date = getDateFromIso8601String(s);
        System.err.println("Got date: " + date);
        if (date != null) {
            if (date.getHours() == 23 && date.getMinutes() == 59 && date.getSeconds() == 59) {
                date.setHours(12);
                date.setMinutes(0);
                date.setSeconds(0);
            }
        }
        return date;
    }

    /**
     * Get current date and time as a string. Used for naming backup files.
     */
    public static String getDateForExport() {
        DateFormat df = new SimpleDateFormat("yyMMdd-HHmm");
        return df.format(newDate());
    }

}
