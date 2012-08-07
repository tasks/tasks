/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.Date;
import java.util.HashMap;

import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.enums.RepeatInterval;
import com.todoroo.astrid.backup.BackupDateUtilities;

@SuppressWarnings("nls")
public class TaskModelForXml extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
            LegacyAbstractController.KEY_ROWID,
            NAME,
            IMPORTANCE,
            ELAPSED_SECONDS,
            ESTIMATED_SECONDS,
            TIMER_START,
            DEFINITE_DUE_DATE,
            PREFERRED_DUE_DATE,
            NOTIFICATIONS,
            PROGRESS_PERCENTAGE,
            COMPLETION_DATE,
            CREATION_DATE,
            HIDDEN_UNTIL,
            NOTES,
            REPEAT,
            FLAGS,
            POSTPONE_COUNT,
            BLOCKING_ON,
            LAST_NOTIFIED,
            NOTIFICATION_FLAGS,
            CALENDAR_URI,
    };
    private final HashMap<String, String> taskAttributesMap;
    public static final String REPEAT_VALUE = "repeat_value";
    public static final String REPEAT_INTERVAL = "repeat_interval";


    private RepeatInterval repeatInterval = null;
    private Integer repeatValue = null;

    // --- constructors

    public TaskModelForXml() {
        super();
        setCreationDate(new Date());
        taskAttributesMap = new HashMap<String, String>(FIELD_LIST.length);
    }

    public TaskModelForXml(Cursor cursor) {
        super(cursor);
        prefetchData(FIELD_LIST);
        taskAttributesMap = new HashMap<String, String>(FIELD_LIST.length);
    }

    /* Safely add a value from a date field (in case of null values) to the
    taskAttributesMap.
     */
    private void safePutDate(String field, Date value) {
        if (value != null) {
            taskAttributesMap.put(field, BackupDateUtilities.getIso8601String(value));
        }
    }

    // --- getters and setters

    @Override
    public Date getCreationDate() {
        return super.getCreationDate();
    }

    /* Build a HashMap of task fields and associated values.
     */
    public HashMap<String, String> getTaskAttributes() {
        taskAttributesMap.put(LegacyAbstractController.KEY_ROWID, getTaskIdentifier().idAsString());
        taskAttributesMap.put(NAME, getName());
        taskAttributesMap.put(IMPORTANCE, getImportance().toString());
        taskAttributesMap.put(ELAPSED_SECONDS, getElapsedSeconds().toString());
        taskAttributesMap.put(ESTIMATED_SECONDS, getEstimatedSeconds().toString());
        safePutDate(TIMER_START, getTimerStart());
        safePutDate(DEFINITE_DUE_DATE, getDefiniteDueDate());
        safePutDate(PREFERRED_DUE_DATE, getPreferredDueDate());
        taskAttributesMap.put(NOTIFICATIONS, getNotificationIntervalSeconds().toString());
        taskAttributesMap.put(PROGRESS_PERCENTAGE, Integer.toString(getProgressPercentage()));
        safePutDate(COMPLETION_DATE, getCompletionDate());
        safePutDate(CREATION_DATE, getCreationDate());
        safePutDate(HIDDEN_UNTIL, getHiddenUntil());
        taskAttributesMap.put(NOTES, getNotes());
        RepeatInfo repeat = getRepeat();
        if (repeat != null) {
            taskAttributesMap.put(REPEAT_VALUE, Integer.toString(repeat.getValue()));
            taskAttributesMap.put(REPEAT_INTERVAL, repeat.getInterval().toString());
        }
        taskAttributesMap.put(FLAGS, Integer.toString(getFlags()));
        taskAttributesMap.put(POSTPONE_COUNT, getPostponeCount().toString());
        taskAttributesMap.put(BLOCKING_ON, Long.toString(getBlockingOn().getId()));
        safePutDate(LAST_NOTIFIED, getLastNotificationDate());
        taskAttributesMap.put(NOTIFICATION_FLAGS, Integer.toString(getNotificationFlags()));
        String calendarUri = getCalendarUri();
        if (calendarUri != null) {
            taskAttributesMap.put(CALENDAR_URI, calendarUri);
        }
        return taskAttributesMap;
    }

    // --- setters

    public boolean setField(String field, String value) {
        boolean success = true;
        if(field.equals(NAME)) {
            setName(value);
        }
        else if(field.equals(NOTES)) {
            setNotes(value);
        }
        else if(field.equals(PROGRESS_PERCENTAGE)) {
            setProgressPercentage(Integer.parseInt(value));
        }
        else if(field.equals(IMPORTANCE)) {
            setImportance(Importance.valueOf(value));
        }
        else if(field.equals(ESTIMATED_SECONDS)) {
            setEstimatedSeconds(Integer.parseInt(value));
        }
        else if(field.equals(ELAPSED_SECONDS)) {
            setElapsedSeconds(Integer.parseInt(value));
        }
        else if(field.equals(TIMER_START)) {
            setTimerStart(BackupDateUtilities.getDateFromIso8601String(value));
        }
        else if(field.equals(DEFINITE_DUE_DATE)) {
            setDefiniteDueDate(BackupDateUtilities.getDateFromIso8601String(value));
        }
        else if(field.equals(PREFERRED_DUE_DATE)) {
            setPreferredDueDate(BackupDateUtilities.getDateFromIso8601String(value));
        }
        else if(field.equals(HIDDEN_UNTIL)) {
            setHiddenUntil(BackupDateUtilities.getDateFromIso8601String(value));
        }
        else if(field.equals(BLOCKING_ON)) {
            setBlockingOn(new TaskIdentifier(Long.parseLong(value)));
        }
        else if(field.equals(POSTPONE_COUNT)) {
            setPostponeCount(Integer.parseInt(value));
        }
        else if(field.equals(NOTIFICATIONS)) {
            setNotificationIntervalSeconds(Integer.parseInt(value));
        }
        else if(field.equals(CREATION_DATE)) {
            setCreationDate(BackupDateUtilities.getDateFromIso8601String(value));
        }
        else if(field.equals(COMPLETION_DATE)) {
            setCompletionDate(BackupDateUtilities.getDateFromIso8601String(value));
        }
        else if(field.equals(NOTIFICATION_FLAGS)) {
            setNotificationFlags(Integer.parseInt(value));
        }
        else if(field.equals(LAST_NOTIFIED)) {
            setLastNotificationTime(BackupDateUtilities.getDateFromIso8601String(value));
        }
        else if(field.equals(REPEAT_INTERVAL)) {
            try {
                setRepeatInterval(RepeatInterval.valueOf(value));
            } catch (Exception e) {
                // bad saving format, old backup
            }
        }
        else if(field.equals(REPEAT_VALUE)) {
            setRepeatValue(Integer.parseInt(value));
        }
        else if(field.equals(FLAGS)) {
            setFlags(Integer.parseInt(value));
        }
        else {
            success = false;
        }
        return success;
    }

    public void setRepeatInterval(RepeatInterval repeatInterval) {
        this.repeatInterval = repeatInterval;
        if (repeatValue != null) {
            setRepeat(new RepeatInfo(repeatInterval, repeatValue));
        }
    }

    public void setRepeatValue(Integer repeatValue) {
        this.repeatValue = repeatValue;
        if (repeatInterval != null) {
            setRepeat(new RepeatInfo(repeatInterval, repeatValue));
        }
    }
}
