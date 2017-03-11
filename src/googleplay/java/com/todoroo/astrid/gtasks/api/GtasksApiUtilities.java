/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.api;

import com.google.api.client.googleapis.testing.json.GoogleJsonResponseExceptionFactoryTesting;
import com.google.api.client.util.DateTime;
import com.todoroo.astrid.data.Task;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import timber.log.Timber;

public class GtasksApiUtilities {

    public static final char NOTE_METADATA_VALUE_SPLIT_CHAR = ';';
    private static final String REMINDER_FLAG_AT = "at";
    private static final String REMINDER_FLAG_AFTER = "after";
    private static final String REMINDER_FLAG_FIVE = "five";
    private static final String REMINDER_FLAG_NONSTOP = "nonstop";

    private static Map<String, Integer> importanceMapping;

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


    public static String unixTimeToGtasksStringTime(long time) {
        if (time <= 0) {
            return null;
        }
        DateTime dateTime = new DateTime(new Date(time), TimeZone.getDefault());
        return dateTime.toStringRfc3339();
    }

    public static long gtasksStringTimeToUnixTime(String gtasksStringTime) {
        if (gtasksStringTime == null || gtasksStringTime.trim().length()==0) {
            return 0;
        }
        try {
            DateTime dateTime = DateTime.parseRfc3339(gtasksStringTime);
            return dateTime.getValue();
        } catch (NumberFormatException e) {
            Timber.e(e, e.getMessage());
            return 0;
        }
    }

    public static Set<String> gtasksTagsToTags(String tagsString) {
        String[] tags = tagsString.split("" + GtasksApiUtilities.NOTE_METADATA_VALUE_SPLIT_CHAR);
        return new HashSet<String>(Arrays.asList(tags));
    }

    public static String tagsToGtaskTags(Collection<String> tags) {
        if (tags!=null && tags.size()>0) {
            StringBuffer buf = new StringBuffer();
            for (String tag : tags) {
                appendValue(buf, tag);
            }
            return buf.toString();
        } else {
            return null;
        }
    }

    public static String importanceToGtasksString(int importance) {
        String result = null;
        for (Map.Entry<String, Integer> e : importanceMapping.entrySet()) {
            if (result == null && Integer.valueOf(importance).equals(e.getValue())) {
                result = e.getKey();
            }
        }
        return result;
    }

    public static int gtasksStringToImportance(String importance, int defaultImportance) {
        Integer result = importanceMapping.get(importance);
        if (result==null) {
            result = defaultImportance;
        }
        return result;
    }

    public static String reminderFlagsToGtasksString(boolean atDeadline, boolean afterDeadline, boolean nonstop, boolean modeFive) {
        StringBuffer buf = new StringBuffer();
        if (atDeadline) {
            appendValue(buf, REMINDER_FLAG_AT);
        }
        if (afterDeadline) {
            appendValue(buf, REMINDER_FLAG_AFTER);
        }
        if (modeFive) {
            appendValue(buf, REMINDER_FLAG_FIVE);
        }  else if (nonstop) {
            appendValue(buf, REMINDER_FLAG_NONSTOP);
        }
        return buf.toString();
    }

    public static int gtasksReminderFlagsTimeToFlags(String flags) {
        List<String> reminderFlags = Arrays.asList(flags.split("" + GtasksApiUtilities.NOTE_METADATA_VALUE_SPLIT_CHAR));
        int result = 0;
        result = result | (reminderFlags.contains(REMINDER_FLAG_AT)?Task.NOTIFY_AT_DEADLINE:0);
        result = result | (reminderFlags.contains(REMINDER_FLAG_AFTER)?Task.NOTIFY_AFTER_DEADLINE:0);
        result = result | (reminderFlags.contains(REMINDER_FLAG_FIVE)?Task.NOTIFY_MODE_FIVE | Task.NOTIFY_MODE_NONSTOP :0);
        result = result | (reminderFlags.contains(REMINDER_FLAG_NONSTOP)?Task.NOTIFY_MODE_NONSTOP:0);
        return result;
    }

    // Helper
    private static void appendValue(StringBuffer buf, String value) {
        if (value!=null && value.trim().length()>0) {
            if (buf.length() > 0) {
                buf.append(NOTE_METADATA_VALUE_SPLIT_CHAR);
            }
            buf.append(value.trim());
        }
    }

    static {
         importanceMapping = new HashMap<>();
        importanceMapping.put("MUST", Task.IMPORTANCE_DO_OR_DIE);
        importanceMapping.put("HIGH", Task.IMPORTANCE_MUST_DO);
        importanceMapping.put("DEFAULT", Task.IMPORTANCE_SHOULD_DO);
        importanceMapping.put("LOW", Task.IMPORTANCE_NONE);
    }




}
