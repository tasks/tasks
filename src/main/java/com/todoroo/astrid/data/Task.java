/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.BuildConfig;
import org.tasks.time.DateTime;

import static org.tasks.date.DateTimeUtils.newDateTime;

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Task extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("tasks", Task.class);

    public static final Uri CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/" +
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
            TABLE, "dueDate", Property.PROP_FLAG_DATE);

    /** Unixtime Task should be hidden until, 0 if not set */
    public static final LongProperty HIDE_UNTIL = new LongProperty(
            TABLE, "hideUntil", Property.PROP_FLAG_DATE);

    /** Unixtime Task was created */
    public static final LongProperty CREATION_DATE = new LongProperty(
            TABLE, "created", Property.PROP_FLAG_DATE);

    /** Unixtime Task was last touched */
    public static final LongProperty MODIFICATION_DATE = new LongProperty(
            TABLE, "modified", Property.PROP_FLAG_DATE);

    /** Unixtime Task was completed. 0 means active */
    public static final LongProperty COMPLETION_DATE = new LongProperty(
            TABLE, "completed", Property.PROP_FLAG_DATE);

    /** Unixtime Task was deleted. 0 means not deleted */
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted", Property.PROP_FLAG_DATE);

    // --- non-core task metadata

    public static final StringProperty NOTES = new StringProperty(
            TABLE, "notes");

    public static final IntegerProperty ESTIMATED_SECONDS = new IntegerProperty(
            TABLE, "estimatedSeconds");

    public static final IntegerProperty ELAPSED_SECONDS = new IntegerProperty(
            TABLE, "elapsedSeconds");

    public static final LongProperty TIMER_START = new LongProperty(
            TABLE, "timerStart", Property.PROP_FLAG_DATE);

    /** Flags for when to send reminders */
    public static final IntegerProperty REMINDER_FLAGS = new IntegerProperty(
            TABLE, "notificationFlags");

    /** Reminder period, in milliseconds. 0 means disabled */
    public static final LongProperty REMINDER_PERIOD = new LongProperty(
            TABLE, "notifications", Property.PROP_FLAG_DATE);

    /** Unixtime the last reminder was triggered */
    public static final LongProperty REMINDER_LAST = new LongProperty(
            TABLE, "lastNotified", Property.PROP_FLAG_DATE);

    /** Unixtime snooze is set (0 -> no snooze) */
    public static final LongProperty REMINDER_SNOOZE = new LongProperty(
            TABLE, "snoozeTime", Property.PROP_FLAG_DATE);

    public static final StringProperty RECURRENCE = new StringProperty(
            TABLE, "recurrence");

    public static final LongProperty REPEAT_UNTIL = new LongProperty(
            TABLE, "repeatUntil", Property.PROP_FLAG_DATE);

    public static final StringProperty CALENDAR_URI = new StringProperty(
            TABLE, "calendarUri");

    // --- for astrid.com

    /** Remote id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME, Property.PROP_FLAG_NULLABLE);

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Task.class);

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

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(TITLE.name, "");
        defaultValues.put(DUE_DATE.name, 0L);
        defaultValues.put(HIDE_UNTIL.name, 0);
        defaultValues.put(COMPLETION_DATE.name, 0);
        defaultValues.put(DELETION_DATE.name, 0);
        defaultValues.put(IMPORTANCE.name, IMPORTANCE_NONE);
        defaultValues.put(CALENDAR_URI.name, "");
        defaultValues.put(RECURRENCE.name, "");
        defaultValues.put(REPEAT_UNTIL.name, 0L);
        defaultValues.put(REMINDER_PERIOD.name, 0);
        defaultValues.put(REMINDER_FLAGS.name, 0);
        defaultValues.put(REMINDER_LAST.name, 0);
        defaultValues.put(REMINDER_SNOOZE.name, 0);
        defaultValues.put(ESTIMATED_SECONDS.name, 0);
        defaultValues.put(ELAPSED_SECONDS.name, 0);
        defaultValues.put(NOTES.name, "");
        defaultValues.put(TIMER_START.name, 0);
        defaultValues.put(UUID.name, NO_UUID);
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
        super(cursor);
    }

    public Task(Task task) {
        super(task);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    public String getUuid() {
        return getUuidHelper(UUID);
    }

    // --- parcelable helpers

    public static final Creator<Task> CREATOR = new ModelCreator<>(Task.class);

    // --- data access methods

    /** Checks whether task is done. Requires COMPLETION_DATE */
    public boolean isCompleted() {
        return getValue(COMPLETION_DATE) > 0;
    }

    /** Checks whether task is deleted. Will return false if DELETION_DATE not read */
    public boolean isDeleted() {
        // assume false if we didn't load deletion date
        if(!containsValue(DELETION_DATE)) {
            return false;
        } else {
            return getValue(DELETION_DATE) > 0;
        }
    }

    /** Checks whether task is hidden. Requires HIDDEN_UNTIL */
    public boolean isHidden() {
        return getValue(HIDE_UNTIL) > DateUtilities.now();
    }

    public boolean hasHideUntilDate() {
        return getValue(HIDE_UNTIL) > 0;
    }

    /** Checks whether task is done. Requires DUE_DATE */
    public boolean hasDueDate() {
        return getValue(DUE_DATE) > 0;
    }

    // --- due and hide until date management

    /** urgency array index -> significance */
    public static final int URGENCY_NONE = 0;

    public static final int URGENCY_TODAY = 1;
    public static final int URGENCY_TOMORROW = 2;
    public static final int URGENCY_DAY_AFTER = 3;
    public static final int URGENCY_NEXT_WEEK = 4;
    public static final int URGENCY_IN_TWO_WEEKS = 5;
    public static final int URGENCY_NEXT_MONTH = 6;
    public static final int URGENCY_SPECIFIC_DAY = 7;
    public static final int URGENCY_SPECIFIC_DAY_TIME = 8;
    /** hide until array index -> significance */
    public static final int HIDE_UNTIL_NONE = 0;

    public static final int HIDE_UNTIL_DUE = 1;
    public static final int HIDE_UNTIL_DAY_BEFORE = 2;
    public static final int HIDE_UNTIL_WEEK_BEFORE = 3;
    public static final int HIDE_UNTIL_SPECIFIC_DAY = 4;
    public static final int HIDE_UNTIL_SPECIFIC_DAY_TIME = 5;
    public static final int HIDE_UNTIL_DUE_TIME = 6;
    /**
     * Creates due date for this task. If this due date has no time associated,
     * we move it to the last millisecond of the day.
     *
     * @param setting
     *            one of the URGENCY_* constants
     * @param customDate
     *            if specific day or day & time is set, this value
     */
    public static long createDueDate(int setting, long customDate) {
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
        case URGENCY_IN_TWO_WEEKS:
            date = DateUtilities.now() + 2 * DateUtilities.ONE_WEEK;
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

        if(date <= 0) {
            return date;
        }

        DateTime dueDate = newDateTime(date).withMillisOfSecond(0);
        if(setting != URGENCY_SPECIFIC_DAY_TIME) {
            dueDate = dueDate
                    .withHourOfDay(12)
                    .withMinuteOfHour(0)
                    .withSecondOfMinute(0); // Seconds == 0 means no due time
        } else {
            dueDate = dueDate.withSecondOfMinute(1); // Seconds > 0 means due time exists
        }
        return dueDate.getMillis();
    }

    /**
     * Create hide until for this task.
     *
     * @param setting
     *            one of the HIDE_UNTIL_* constants
     * @param customDate
     *            if specific day is set, this value
     */
    public long createHideUntil(int setting, long customDate) {
        long date;

        switch(setting) {
        case HIDE_UNTIL_NONE:
            return 0;
        case HIDE_UNTIL_DUE:
        case HIDE_UNTIL_DUE_TIME:
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

        if(date <= 0) {
            return date;
        }

        DateTime hideUntil = newDateTime(date).withMillisOfSecond(0); // get rid of millis
        if(setting != HIDE_UNTIL_SPECIFIC_DAY_TIME && setting != HIDE_UNTIL_DUE_TIME) {
            hideUntil = hideUntil
                    .withHourOfDay(0)
                    .withMinuteOfHour(0)
                    .withSecondOfMinute(0);
        } else {
            hideUntil = hideUntil.withSecondOfMinute(1);
        }
        return hideUntil.getMillis();
    }

    /**
     * Checks whether this due date has a due time or only a date
     */
    public boolean hasDueTime() {
        return hasDueDate() && hasDueTime(getDueDate());
    }

    public boolean isOverdue() {
        long dueDate = getDueDate();
        long compareTo = hasDueTime() ? DateUtilities.now() : DateUtilities.getStartOfDay(DateUtilities.now());

        return dueDate < compareTo && !isCompleted();
    }

    public boolean repeatAfterCompletion() {
        return getRecurrence().contains("FROM=COMPLETION");
    }

    public String sanitizedRecurrence() {
        return getRecurrence().replaceAll("BYDAY=;", "").replaceAll(";?FROM=[^;]*", "");  //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * Checks whether provided due date has a due time or only a date
     */
    public static boolean hasDueTime(long dueDate) {
        return dueDate > 0 && (dueDate % 60000 > 0);
    }

    public Long getDueDate() {
        return getValue(DUE_DATE);
    }

    public void setDueDate(Long dueDate) {
        setValue(DUE_DATE, dueDate);
    }

    public void setDueDateAdjustingHideUntil(Long dueDate) {
        long oldDueDate = getValue(DUE_DATE);
        if (oldDueDate > 0) {
            long hideUntil = getValue(HIDE_UNTIL);
            if (hideUntil > 0) {
                setHideUntil(dueDate > 0 ? hideUntil + dueDate - oldDueDate : 0);
            }
        }
        setDueDate(dueDate);
    }

    public String getRecurrence() {
        return getValue(RECURRENCE);
    }

    public void setRecurrence(String recurrence) {
        setValue(RECURRENCE, recurrence);
    }

    public Long getCreationDate() {
        return getValue(CREATION_DATE);
    }

    public void setCreationDate(Long creationDate) {
        setValue(CREATION_DATE, creationDate);
    }

    public String getUUID() {
        return getValue(UUID);
    }

    public String getTitle() {
        return getValue(TITLE);
    }

    public void setTitle(String title) {
        setValue(TITLE, title);
    }

    public Long getDeletionDate() {
        return getValue(DELETION_DATE);
    }

    public void setDeletionDate(Long deletionDate) {
        setValue(DELETION_DATE, deletionDate);
    }

    public Long getHideUntil() {
        return getValue(HIDE_UNTIL);
    }

    public void setHideUntil(Long hideUntil) {
        setValue(HIDE_UNTIL, hideUntil);
    }

    public Long getReminderLast() {
        return getValue(REMINDER_LAST);
    }

    public void setReminderLast(Long reminderLast) {
        setValue(REMINDER_LAST, reminderLast);
    }

    public Long getReminderSnooze() {
        return getValue(REMINDER_SNOOZE);
    }

    public void setReminderSnooze(Long reminderSnooze) {
        setValue(REMINDER_SNOOZE, reminderSnooze);
    }

    public Integer getElapsedSeconds() {
        return getValue(ELAPSED_SECONDS);
    }

    public Long getTimerStart() {
        return getValue(TIMER_START);
    }

    public void setTimerStart(Long timerStart) {
        setValue(TIMER_START, timerStart);
    }

    public Long getRepeatUntil() {
        return getValue(REPEAT_UNTIL);
    }

    public void setRepeatUntil(Long repeatUntil) {
        setValue(REPEAT_UNTIL, repeatUntil);
    }

    public String getCalendarURI() {
        return getValue(CALENDAR_URI);
    }

    public Integer getImportance() {
        return getValue(IMPORTANCE);
    }

    public void setImportance(Integer importance) {
        setValue(IMPORTANCE, importance);
    }

    public Long getCompletionDate() {
        return getValue(COMPLETION_DATE);
    }

    public void setCompletionDate(Long completionDate) {
        setValue(COMPLETION_DATE, completionDate);
    }

    public String getNotes() {
        return getValue(NOTES);
    }

    public void setNotes(String notes) {
        setValue(NOTES, notes);
    }

    public void setModificationDate(Long modificationDate) {
        setValue(MODIFICATION_DATE, modificationDate);
    }

    public Integer getReminderFlags() {
        return getValue(REMINDER_FLAGS);
    }

    public void setReminderFlags(Integer reminderFlags) {
        setValue(REMINDER_FLAGS, reminderFlags);
    }

    public Long getReminderPeriod() {
        return getValue(REMINDER_PERIOD);
    }

    public void setReminderPeriod(Long reminderPeriod) {
        setValue(REMINDER_PERIOD, reminderPeriod);
    }

    public Integer getEstimatedSeconds() {
        return getValue(ESTIMATED_SECONDS);
    }

    public void setElapsedSeconds(Integer elapsedSeconds) {
        setValue(ELAPSED_SECONDS, elapsedSeconds);
    }

    public void setEstimatedSeconds(Integer estimatedSeconds) {
        setValue(ESTIMATED_SECONDS, estimatedSeconds);
    }

    public void setCalendarUri(String calendarUri) {
        setValue(CALENDAR_URI, calendarUri);
    }

    public void setID(Long id) {
        setValue(ID, id);
    }

    public boolean isNotifyModeNonstop() {
        return isReminderFlagSet(Task.NOTIFY_MODE_NONSTOP);
    }

    public boolean isNotifyModeFive() {
        return isReminderFlagSet(Task.NOTIFY_MODE_FIVE);
    }

    public boolean isNotifyAfterDeadline() {
        return isReminderFlagSet(Task.NOTIFY_AFTER_DEADLINE);
    }

    public boolean isNotifyAtDeadline() {
        return isReminderFlagSet(Task.NOTIFY_AT_DEADLINE);
    }

    private boolean isReminderFlagSet(int flag) {
        return (getReminderFlags() & flag) > 0;
    }

    public boolean hasRandomReminder() {
        return getReminderPeriod() > 0;
    }
}
