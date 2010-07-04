/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.model;


import java.util.Date;

import android.content.ContentValues;
import android.content.res.Resources;

import com.timsu.astrid.R;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.DateUtilities;

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class Task extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("tasks", Task.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Name of Task */
    public static final StringProperty TITLE = new StringProperty(
            TABLE, "title");

    /** Importance of Task (see importance flags) */
    public static final IntegerProperty IMPORTANCE = new IntegerProperty(
            TABLE, "importance");

    /** Unixtime Task is due, 0 if not set */
    public static final LongProperty DUE_DATE = new LongProperty(
            TABLE, "dueDate");

    /** Unixtime Task should be hidden until, 0 if not set */
    public static final LongProperty HIDE_UNTIL = new LongProperty(
            TABLE, "hideUntil");

    /** Unixtime Task was created */
    public static final LongProperty CREATION_DATE = new LongProperty(
            TABLE, "created");

    /** Unixtime Task was last touched */
    public static final LongProperty MODIFICATION_DATE = new LongProperty(
            TABLE, "modified");

    /** Unixtime Task was completed. 0 means active */
    public static final LongProperty COMPLETION_DATE = new LongProperty(
            TABLE, "completed");

    /** Unixtime Task was deleted. 0 means not deleted */
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted");

    // --- for migration purposes from astrid 2 (eventually we will want to
    //     move these into the metadata table and treat them as plug-ins

    public static final StringProperty NOTES = new StringProperty(
            TABLE, "notes");

    public static final IntegerProperty ESTIMATED_SECONDS = new IntegerProperty(
            TABLE, "estimatedSeconds");

    public static final IntegerProperty ELAPSED_SECONDS = new IntegerProperty(
            TABLE, "elapsedSeconds");

    public static final IntegerProperty TIMER_START = new IntegerProperty(
            TABLE, "timerStart");

    public static final IntegerProperty POSTPONE_COUNT = new IntegerProperty(
            TABLE, "postponeCount");

    /** Flags for when to send reminders */
    public static final IntegerProperty REMINDER_FLAGS = new IntegerProperty(
            TABLE, "notificationFlags");

    /** Reminder period, in milliseconds. 0 means disabled */
    public static final LongProperty REMINDER_PERIOD = new LongProperty(
            TABLE, "notifications");

    /** Unixtime the last reminder was triggered */
    public static final LongProperty REMINDER_LAST = new LongProperty(
            TABLE, "lastNotified");

    public static final IntegerProperty REPEAT = new IntegerProperty(
            TABLE, "repeat");

    public static final IntegerProperty FLAGS = new IntegerProperty(
            TABLE, "flags");

    public static final StringProperty CALENDAR_URI = new StringProperty(
            TABLE, "calendarUri");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Task.class);

    // --- flags

    // --- notification flags

    /** whether to send a reminder at deadline */
    public static final int NOTIFY_AT_DEADLINE = 1 << 1;

    /** whether to send reminders while task is overdue */
    public static final int NOTIFY_AFTER_DEADLINE = 1 << 2;

    /** reminder mode non-stop */
    public static final int NOTIFY_NONSTOP = 1 << 3;

    // --- importance settings

    public static final int IMPORTANCE_DO_OR_DIE = 0;
    public static final int IMPORTANCE_MUST_DO = 1;
    public static final int IMPORTANCE_SHOULD_DO = 2;
    public static final int IMPORTANCE_NONE = 3;

    /**
     * @return colors that correspond to importance values
     */
    public static int[] getImportanceColors(Resources r) {
        return new int[] {
                r.getColor(R.color.importance_1),
                r.getColor(R.color.importance_2),
                r.getColor(R.color.importance_3),
                r.getColor(R.color.importance_4),
        };
    }

    public static final int IMPORTANCE_MOST = IMPORTANCE_DO_OR_DIE;
    public static final int IMPORTANCE_LEAST = IMPORTANCE_NONE;

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(TITLE.name, "");
        defaultValues.put(DUE_DATE.name, 0);
        defaultValues.put(HIDE_UNTIL.name, 0);
        defaultValues.put(COMPLETION_DATE.name, 0);
        defaultValues.put(DELETION_DATE.name, 0);
        defaultValues.put(IMPORTANCE.name, IMPORTANCE_NONE);

        defaultValues.put(CALENDAR_URI.name, "");
        defaultValues.put(REPEAT.name, 0);
        defaultValues.put(REMINDER_PERIOD.name, 0);
        defaultValues.put(REMINDER_FLAGS.name, 0);
        defaultValues.put(ESTIMATED_SECONDS.name, 0);
        defaultValues.put(ELAPSED_SECONDS.name, 0);
        defaultValues.put(POSTPONE_COUNT.name, 0);
        defaultValues.put(NOTES.name, "");
        defaultValues.put(TIMER_START.name, 0);
    }

    private static boolean defaultValuesLoaded = false;

    public static ContentValues getStaticDefaultValues() {
        return defaultValues;
    }

    /**
     * Call to load task default values from preferences.
     */
    public static void refreshDefaultValues() {
        /*defaultValues.put(URGENCY.name,
                Preferences.getIntegerFromString(R.string.EPr_default_urgency_key));
        defaultValues.put(IMPORTANCE.name,
                Preferences.getIntegerFromString(R.string.EPr_default_importance_key));*/
        defaultValuesLoaded = true;
    }

    @Override
    public ContentValues getDefaultValues() {
        // if refreshDefaultValues has never been called, call it
        if(!defaultValuesLoaded) {
            refreshDefaultValues();
        }

        return defaultValues;
    }

    // --- data access boilerplate

    public Task() {
        super();
    }

    public Task(TodorooCursor<Task> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<Task> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    private static final Creator<Task> CREATOR = new ModelCreator<Task>(Task.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

    // --- data access methods

    /** Checks whether task is done. Requires COMPLETION_DATE */
    public boolean isCompleted() {
        return getValue(COMPLETION_DATE) > 0;
    }

    /** Checks whether task is deleted. Will return false if DELETION_DATE not read */
    public boolean isDeleted() {
        try {
            return getValue(DELETION_DATE) > 0;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    /** Checks whether task is hidden. Requires HIDDEN_UNTIL */
    public boolean isHidden() {
    	return getValue(HIDE_UNTIL) > DateUtilities.now();
    }

    /** Checks whether task is done. Requires DUE_DATE */
    public boolean hasDueDate() {
        return getValue(DUE_DATE) > 0;
    }

    /**
     * @return true if hours, minutes, and seconds indicate end of day
     */
    private static boolean isEndOfDay(Date date) {
        return date.getHours() == 23 && date.getMinutes() == 59 &&
            date.getSeconds() == 59;
    }

    /**
     * Sets due date for this task. If this due date has no time associated,
     * we move it to the last millisecond of the day.
     *
     * @param date
     * @param hasDueTime
     */
    public void setDueDateAndTime(Date dueDate, boolean hasDueTime) {
        if(!hasDueTime) {
            dueDate.setHours(23);
            dueDate.setMinutes(59);
            dueDate.setSeconds(59);
        } else if(isEndOfDay(dueDate)) {
            dueDate.setSeconds(58);
        }
        setValue(Task.DUE_DATE, dueDate.getTime());
    }

    /**
     * Checks whether this due date has a due time or only a date
     */
    public boolean hasDueTime() {
        return isEndOfDay(new Date(getValue(DUE_DATE)));
    }

    /**
     * Returns the set state of the given flag on the given property
     * @param property
     * @param flag
     * @return
     */
    public boolean getFlag(IntegerProperty property, int flag) {
        return (getValue(property) & flag) > 0;
    }
    
    /**
     * @return repeat data structure. Requires REPEAT
     */
    public RepeatInfo getRepeatInfo() {
        return RepeatInfo.fromSingleField(getValue(Task.REPEAT));
    }
}
