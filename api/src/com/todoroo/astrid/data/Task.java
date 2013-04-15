/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
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

    /** model class for entries in the outstanding table */
    public static final Class<? extends OutstandingEntry<Task>> OUTSTANDING_MODEL = TaskOutstanding.class;

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
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

    /** Cached Details Column - built from add-on detail exposers. A null
     * value means there is no value in the cache and it needs to be
     * refreshed */
    public static final StringProperty DETAILS = new StringProperty(
            TABLE, "details");

    /** Date details were last updated */
    public static final LongProperty DETAILS_DATE = new LongProperty(
            TABLE, "detailsDate", Property.PROP_FLAG_DATE);

    @Deprecated
    public static final IntegerProperty FLAGS = new IntegerProperty(
            TABLE, "flags");

    public static final IntegerProperty IS_PUBLIC = new IntegerProperty(
            TABLE, "is_public", Property.PROP_FLAG_BOOLEAN);

    public static final IntegerProperty IS_READONLY = new IntegerProperty(
            TABLE, "is_readonly", Property.PROP_FLAG_BOOLEAN);

    // --- non-core task metadata

    public static final StringProperty NOTES = new StringProperty(
            TABLE, "notes");

    public static final IntegerProperty ESTIMATED_SECONDS = new IntegerProperty(
            TABLE, "estimatedSeconds");

    public static final IntegerProperty ELAPSED_SECONDS = new IntegerProperty(
            TABLE, "elapsedSeconds");

    public static final LongProperty TIMER_START = new LongProperty(
            TABLE, "timerStart", Property.PROP_FLAG_DATE);

    public static final IntegerProperty POSTPONE_COUNT = new IntegerProperty(
            TABLE, "postponeCount");

    /** Flags for when to send reminders */
    public static final IntegerProperty REMINDER_FLAGS = new IntegerProperty(
            TABLE, "notificationFlags");

    /** Reminder period, in milliseconds. 0 means disabled */
    public static final LongProperty REMINDER_PERIOD = new LongProperty(
            TABLE, "notifications", Property.PROP_FLAG_DATE);

    /** Unixtime the last reminder was triggered */
    public static final LongProperty REMINDER_LAST = new LongProperty(
            TABLE, "lastNotified", Property.PROP_FLAG_DATE);

    /** What kind of reminder the last reminder was: private task,
     *  social with no faces, social with faces */
    public static final StringProperty SOCIAL_REMINDER = new StringProperty(
            TABLE, "socialReminder");

    /** Unixtime snooze is set (0 -> no snooze) */
    public static final LongProperty REMINDER_SNOOZE = new LongProperty(
            TABLE, "snoozeTime", Property.PROP_FLAG_DATE);

    public static final StringProperty RECURRENCE = new StringProperty(
            TABLE, "recurrence");

    public static final LongProperty REPEAT_UNTIL = new LongProperty(
            TABLE, "repeatUntil", Property.PROP_FLAG_DATE);

    public static final StringProperty CALENDAR_URI = new StringProperty(
            TABLE, "calendarUri");

    public static final StringProperty CLASSIFICATION = new StringProperty(
            TABLE, "classification");

    // --- for astrid.com

    /** Remote id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME, Property.PROP_FLAG_NULLABLE);

    /** Assigned user id */
    public static final StringProperty USER_ID = new StringProperty(
            TABLE, USER_ID_PROPERTY_NAME, Property.PROP_FLAG_USER_ID);

    /** User Object (JSON) */
    @Deprecated public static final StringProperty USER = new StringProperty(
            TABLE, USER_JSON_PROPERTY_NAME);

    /** Creator user id */
    public static final StringProperty CREATOR_ID = new StringProperty(
            TABLE, "creatorId", Property.PROP_FLAG_USER_ID);

    /** Last Sync date */
    @Deprecated
    public static final LongProperty LAST_SYNC = new LongProperty(
            TABLE, "lastSync", Property.PROP_FLAG_DATE);

    /** Pushed at date */
    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, PUSHED_AT_PROPERTY_NAME, Property.PROP_FLAG_DATE);

    /** Attachments pushed at date */
    public static final LongProperty ATTACHMENTS_PUSHED_AT = new LongProperty(
            TABLE, "attachments_pushed_at", Property.PROP_FLAG_DATE);

    /** User activities pushed at date */
    public static final LongProperty USER_ACTIVITIES_PUSHED_AT = new LongProperty(
            TABLE, "activities_pushed_at", Property.PROP_FLAG_DATE);

    /** History fetch time */
    public static final LongProperty HISTORY_FETCH_DATE = new LongProperty(
            TABLE, "historyFetch");

    /** History has more*/
    public static final IntegerProperty HISTORY_HAS_MORE = new IntegerProperty(
            TABLE, "historyHasMore");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Task.class);

    // --- flags

    /** whether repeat occurs relative to completion date instead of due date */
    @Deprecated public static final int FLAG_REPEAT_AFTER_COMPLETION = 1 << 1;

    /** whether task is read-only */
    @Deprecated public static final int FLAG_IS_READONLY = 1 << 2;

    /** whether a task is public */
    @Deprecated public static final int FLAG_PUBLIC = 1 << 3;

    // --- user id special values

    /** user id = doesn't exist/ignore it. For filtering in tags */
    public static final String USER_ID_IGNORE = "-3";

    /** user id = read user email value */
    public static final String USER_ID_EMAIL = "-2";

    /** user id = unassigned */
    public static final String USER_ID_UNASSIGNED = "-1";

    /** user id = myself */
    public static final String USER_ID_SELF = "0";

    public static boolean isRealUserId(String userId) {
        if (userId == null)
            return false;
        return !(Task.USER_ID_SELF.equals(userId) ||
                Task.USER_ID_UNASSIGNED.equals(userId) ||
                Task.USER_ID_EMAIL.equals(userId) ||
                Task.USER_ID_IGNORE.equals(userId));
    }

    public static boolean userIdIsEmail(String userId) {
        if (userId == null)
            return false;
        return userId.indexOf('@') >= 0;
    }

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

    // --- social reminder types

    public static final String REMINDER_SOCIAL_UNSEEN = "unseen";
    public static final String REMINDER_SOCIAL_PRIVATE = "private";
    public static final String REMINDER_SOCIAL_NO_FACES = "no_faces";
    public static final String REMINDER_SOCIAL_FACES = "faces";

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
        defaultValues.put(REPEAT_UNTIL.name, 0);
        defaultValues.put(REMINDER_PERIOD.name, 0);
        defaultValues.put(REMINDER_FLAGS.name, 0);
        defaultValues.put(REMINDER_LAST.name, 0);
        defaultValues.put(SOCIAL_REMINDER.name, REMINDER_SOCIAL_UNSEEN);
        defaultValues.put(REMINDER_SNOOZE.name, 0);
        defaultValues.put(ESTIMATED_SECONDS.name, 0);
        defaultValues.put(ELAPSED_SECONDS.name, 0);
        defaultValues.put(POSTPONE_COUNT.name, 0);
        defaultValues.put(NOTES.name, "");
        defaultValues.put(FLAGS.name, 0);
        defaultValues.put(TIMER_START.name, 0);
        defaultValues.put(DETAILS.name, (String)null);
        defaultValues.put(DETAILS_DATE.name, 0);
        defaultValues.put(IS_PUBLIC.name, 0);
        defaultValues.put(IS_READONLY.name, 0);
        defaultValues.put(CLASSIFICATION.name, "");
        defaultValues.put(HISTORY_FETCH_DATE.name, 0);
        defaultValues.put(HISTORY_HAS_MORE.name, 0);

        defaultValues.put(LAST_SYNC.name, 0);
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(USER_ID.name, "0");
        defaultValues.put(CREATOR_ID.name, 0);
        defaultValues.put(USER.name, "");
//        defaultValues.put(USER_EMAIL.name, "");
        defaultValues.put(PUSHED_AT.name, 0L);
        defaultValues.put(ATTACHMENTS_PUSHED_AT.name, 0L);
        defaultValues.put(USER_ACTIVITIES_PUSHED_AT.name, 0L);
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

    @Override
    public String getUuid() {
        return getUuidHelper(UUID);
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

        if(date <= 0)
            return date;

        Date dueDate = new Date(date / 1000L * 1000L); // get rid of millis
        if(setting != URGENCY_SPECIFIC_DAY_TIME) {
            dueDate.setHours(12);
            dueDate.setMinutes(0);
            dueDate.setSeconds(0); // Seconds == 0 means no due time
        } else {
            dueDate.setSeconds(1); // Seconds > 0 means due time exists
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

        if(date <= 0)
            return date;

        Date hideUntil = new Date(date / 1000L * 1000L); // get rid of millis
        if(setting != HIDE_UNTIL_SPECIFIC_DAY_TIME && setting != HIDE_UNTIL_DUE_TIME) {
            hideUntil.setHours(0);
            hideUntil.setMinutes(0);
            hideUntil.setSeconds(0);
        } else {
            hideUntil.setSeconds(1);
        }
        return hideUntil.getTime();
    }

    /**
     * Checks whether this due date has a due time or only a date
     */
    public boolean hasDueTime() {
        if(!hasDueDate())
            return false;
        return hasDueTime(getValue(Task.DUE_DATE));
    }

    public boolean isOverdue() {
        long dueDate = getValue(Task.DUE_DATE);
        long compareTo = hasDueTime() ? DateUtilities.now() : DateUtilities.getStartOfDay(DateUtilities.now());

        return dueDate < compareTo;
    }

    public boolean isEditable() {
        return (getValue(Task.IS_READONLY) == 0) &&
                !(getValue(Task.IS_PUBLIC) == 1 && !Task.USER_ID_SELF.equals(getValue(Task.USER_ID)));
    }

    public boolean repeatAfterCompletion() {
        return getValue(Task.RECURRENCE).contains("FROM=COMPLETION");
    }

    public String sanitizedRecurrence() {
        return getValue(Task.RECURRENCE).replaceAll("BYDAY=;","").replaceAll(";?FROM=[^;]*", "");  //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * Checks whether provided due date has a due time or only a date
     */
    public static boolean hasDueTime(long dueDate) {
        return dueDate > 0 && (dueDate % 60000 > 0);
    }

}
