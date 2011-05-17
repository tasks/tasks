/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.data;


import java.util.Date;

import android.content.ContentValues;
import android.content.res.Resources;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.R;

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class Task extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("tasks", Task.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.PACKAGE + "/" +
            TABLE.name);

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

    /** Cached Details Column - built from add-on detail exposers. A null
     * value means there is no value in the cache and it needs to be
     * refreshed */
    public static final StringProperty DETAILS = new StringProperty(
            TABLE, "details");

    /** Date details were last updated */
    public static final LongProperty DETAILS_DATE = new LongProperty(
            TABLE, "detailsDate");

    public static final IntegerProperty FLAGS = new IntegerProperty(
            TABLE, "flags");

    // --- non-core task metadata

    public static final StringProperty NOTES = new StringProperty(
            TABLE, "notes");

    public static final IntegerProperty ESTIMATED_SECONDS = new IntegerProperty(
            TABLE, "estimatedSeconds");

    public static final IntegerProperty ELAPSED_SECONDS = new IntegerProperty(
            TABLE, "elapsedSeconds");

    public static final LongProperty TIMER_START = new LongProperty(
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

    /** Unixtime snooze is set (0 -> no snooze) */
    public static final LongProperty REMINDER_SNOOZE = new LongProperty(
            TABLE, "snoozeTime");

    public static final StringProperty RECURRENCE = new StringProperty(
            TABLE, "recurrence");

    public static final StringProperty CALENDAR_URI = new StringProperty(
            TABLE, "calendarUri");

    // --- for astrid.com

    /** Remote id */
    public static final LongProperty REMOTE_ID = new LongProperty(
            TABLE, REMOTE_ID_PROPERTY_NAME);

    /** Assigned user id */
    public static final LongProperty USER_ID = new LongProperty(
            TABLE, USER_ID_PROPERTY_NAME);

    /** User Object (JSON) */
    public static final StringProperty USER = new StringProperty(
            TABLE, USER_JSON_PROPERTY_NAME);

    /** Creator user id */
    public static final LongProperty CREATOR_ID = new LongProperty(
            TABLE, "creatorId");

    public static final StringProperty SHARED_WITH = new StringProperty(
            TABLE, "sharedWith");

    /** Comment Count */
    public static final IntegerProperty COMMENT_COUNT = new IntegerProperty(
            TABLE, "commentCount");

    /** Last Sync date */
    public static final LongProperty LAST_SYNC = new LongProperty(
            TABLE, "lastSync");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Task.class);

    // --- flags

    /** whether repeat occurs relative to completion date instead of due date */
    public static final int FLAG_REPEAT_AFTER_COMPLETION = 1 << 1;

    /** whether task is read-only */
    public static final int FLAG_IS_READONLY = 1 << 2;

    // --- notification flags

    /** whether to send a reminder at deadline */
    public static final int NOTIFY_AT_DEADLINE = 1 << 1;

    /** whether to send reminders while task is overdue */
    public static final int NOTIFY_AFTER_DEADLINE = 1 << 2;

    /** reminder mode non-stop */
    public static final int NOTIFY_MODE_NONSTOP = 1 << 3;

    /** reminder mode five times (exclusive with non-stop) */
    public static final int NOTIFY_MODE_FIVE = 1 << 4;

    // --- importance settings (note: importance > 3 are supported via plugin)

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
                r.getColor(R.color.importance_5),
                r.getColor(R.color.importance_6),
        };
    }

    public static int IMPORTANCE_MOST = IMPORTANCE_DO_OR_DIE;
    public static int IMPORTANCE_LEAST = IMPORTANCE_NONE;

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
        defaultValues.put(RECURRENCE.name, "");
        defaultValues.put(REMINDER_PERIOD.name, 0);
        defaultValues.put(REMINDER_FLAGS.name, 0);
        defaultValues.put(REMINDER_LAST.name, 0);
        defaultValues.put(REMINDER_SNOOZE.name, 0);
        defaultValues.put(ESTIMATED_SECONDS.name, 0);
        defaultValues.put(ELAPSED_SECONDS.name, 0);
        defaultValues.put(POSTPONE_COUNT.name, 0);
        defaultValues.put(NOTES.name, "");
        defaultValues.put(FLAGS.name, 0);
        defaultValues.put(TIMER_START.name, 0);
        defaultValues.put(DETAILS.name, (String)null);
        defaultValues.put(DETAILS_DATE.name, 0);

        defaultValues.put(LAST_SYNC.name, 0);
        defaultValues.put(REMOTE_ID.name, 0);
        defaultValues.put(USER_ID.name, 0);
        defaultValues.put(USER.name, "");
        defaultValues.put(SHARED_WITH.name, "");
    }

    @Override
    public ContentValues getDefaultValues() {
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

    public static final Creator<Task> CREATOR = new ModelCreator<Task>(Task.class);

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
        // assume false if we didn't load deletion date
        if(!containsValue(DELETION_DATE))
            return false;
        else
            return getValue(DELETION_DATE) > 0;
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
     * Returns the set state of the given flag on the given property
     * @param property
     * @param flag
     * @return
     */
    @Override
    public boolean getFlag(IntegerProperty property, int flag) {
        return (getValue(property) & flag) > 0;
    }

    // --- due and hide until date management

    /** urgency array index -> significance */
    public static final int URGENCY_NONE = 0;
    public static final int URGENCY_TODAY = 1;
    public static final int URGENCY_TOMORROW = 2;
    public static final int URGENCY_DAY_AFTER = 3;
    public static final int URGENCY_NEXT_WEEK = 4;
    public static final int URGENCY_NEXT_MONTH = 5;
    public static final int URGENCY_SPECIFIC_DAY = 6;
    public static final int URGENCY_SPECIFIC_DAY_TIME = 7;

    /** hide until array index -> significance */
    public static final int HIDE_UNTIL_NONE = 0;
    public static final int HIDE_UNTIL_DUE = 1;
    public static final int HIDE_UNTIL_DAY_BEFORE = 2;
    public static final int HIDE_UNTIL_WEEK_BEFORE = 3;
    public static final int HIDE_UNTIL_SPECIFIC_DAY = 4;
    public static final int HIDE_UNTIL_SPECIFIC_DAY_TIME = 5;

    /**
     * Creates due date for this task. If this due date has no time associated,
     * we move it to the last millisecond of the day.
     *
     * @param setting
     *            one of the URGENCY_* constants
     * @param customDate
     *            if specific day or day & time is set, this value
     */
    public long createDueDate(int setting, long customDate) {
        long date;

        switch(setting) {
        case URGENCY_NONE:
            date = 0;
            break;
        case URGENCY_TODAY:
            date = DateUtilities.now();
            break;
        case URGENCY_TOMORROW:
            date = DateUtilities.now() + DateUtilities.ONE_DAY;
            break;
        case URGENCY_DAY_AFTER:
            date = DateUtilities.now() + 2 * DateUtilities.ONE_DAY;
            break;
        case URGENCY_NEXT_WEEK:
            date = DateUtilities.now() + DateUtilities.ONE_WEEK;
            break;
        case URGENCY_NEXT_MONTH:
            date = DateUtilities.oneMonthFromNow();
            break;
        case URGENCY_SPECIFIC_DAY:
        case URGENCY_SPECIFIC_DAY_TIME:
            date = customDate;
            break;
        default:
            throw new IllegalArgumentException("Unknown setting " + setting);
        }

        if(date <= 0)
            return date;

        Date dueDate = new Date(date / 1000L * 1000L); // get rid of millis
        if(setting != URGENCY_SPECIFIC_DAY_TIME) {
            dueDate.setHours(23);
            dueDate.setMinutes(59);
            dueDate.setSeconds(59);
        } else if(isEndOfDay(dueDate)) {
            dueDate.setSeconds(58);
        }
        return dueDate.getTime();
    }

    /**
     * Create hide until for this task.
     *
     * @param setting
     *            one of the HIDE_UNTIL_* constants
     * @param customDate
     *            if specific day is set, this value
     * @return
     */
    public long createHideUntil(int setting, long customDate) {
        long date;

        switch(setting) {
        case HIDE_UNTIL_NONE:
            return 0;
        case HIDE_UNTIL_DUE:
            date = getValue(DUE_DATE);
            break;
        case HIDE_UNTIL_DAY_BEFORE:
            date = getValue(DUE_DATE) - DateUtilities.ONE_DAY;
            break;
        case HIDE_UNTIL_WEEK_BEFORE:
            date = getValue(DUE_DATE) - DateUtilities.ONE_WEEK;
            break;
        case HIDE_UNTIL_SPECIFIC_DAY:
        case HIDE_UNTIL_SPECIFIC_DAY_TIME:
            date = customDate;
            break;
        default:
            throw new IllegalArgumentException("Unknown setting " + setting);
        }

        if(date <= 0)
            return date;

        Date hideUntil = new Date(date / 1000L * 1000L); // get rid of millis
        if(setting != HIDE_UNTIL_SPECIFIC_DAY_TIME) {
            hideUntil.setHours(0);
            hideUntil.setMinutes(0);
            hideUntil.setSeconds(0);
        }
        return hideUntil.getTime();
    }

    /**
     * @return true if hours, minutes, and seconds indicate end of day
     */
    private static boolean isEndOfDay(Date date) {
        int hours = date.getHours();
        int minutes = date.getMinutes();
        int seconds = date.getSeconds();
        return hours == 23 && minutes == 59 && seconds == 59;
    }

    /**
     * Checks whether this due date has a due time or only a date
     */
    public boolean hasDueTime() {
        if(!hasDueDate())
            return false;
        return hasDueTime(getValue(Task.DUE_DATE));
    }

    /**
     * Checks whether provided due date has a due time or only a date
     */
    public static boolean hasDueTime(long dueDate) {
        return !isEndOfDay(new Date(dueDate));
    }

}
