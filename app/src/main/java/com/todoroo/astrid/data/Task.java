/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.common.base.Strings;
import com.google.ical.values.RRule;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.backup.XmlReader;
import org.tasks.backup.XmlWriter;
import org.tasks.data.GoogleTask;
import org.tasks.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.date.DateTimeUtils.newDateTime;

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Entity(tableName = "tasks",
        indices = @Index(name = "t_rid", value = "remoteId", unique = true))
public class Task extends AbstractModel implements Parcelable {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("tasks", Task.class);

    // --- properties

    /** ID */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public Long id = NO_ID;
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Name of Task */
    @ColumnInfo(name = "title")
    public String title = "";
    public static final StringProperty TITLE = new StringProperty(
            TABLE, "title");

    /** Importance of Task (see importance flags) */
    @ColumnInfo(name = "importance")
    public Integer importance = IMPORTANCE_NONE;
    public static final IntegerProperty IMPORTANCE = new IntegerProperty(
            TABLE, "importance");

    /** Unixtime Task is due, 0 if not set */
    @ColumnInfo(name = "dueDate")
    public Long dueDate = 0L;
    public static final LongProperty DUE_DATE = new LongProperty(
            TABLE, "dueDate");

    /** Unixtime Task should be hidden until, 0 if not set */
    @ColumnInfo(name = "hideUntil")
    public Long hideUntil = 0L;
    public static final LongProperty HIDE_UNTIL = new LongProperty(
            TABLE, "hideUntil");

    /** Unixtime Task was created */
    @ColumnInfo(name = "created")
    public Long created = 0L;
    public static final LongProperty CREATION_DATE = new LongProperty(
            TABLE, "created");

    /** Unixtime Task was last touched */
    @ColumnInfo(name = "modified")
    public Long modified = 0L;
    public static final LongProperty MODIFICATION_DATE = new LongProperty(
            TABLE, "modified");

    /** Unixtime Task was completed. 0 means active */
    @ColumnInfo(name = "completed")
    public Long completed = 0L;
    public static final LongProperty COMPLETION_DATE = new LongProperty(
            TABLE, "completed");

    /** Unixtime Task was deleted. 0 means not deleted */
    @ColumnInfo(name = "deleted")
    public Long deleted = 0L;
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted");

    // --- non-core task metadata

    @ColumnInfo(name = "notes")
    public String notes = "";
    public static final StringProperty NOTES = new StringProperty(
            TABLE, "notes");

    @ColumnInfo(name = "estimatedSeconds")
    public Integer estimatedSeconds = 0;
    public static final IntegerProperty ESTIMATED_SECONDS = new IntegerProperty(
            TABLE, "estimatedSeconds");

    @ColumnInfo(name = "elapsedSeconds")
    public Integer elapsedSeconds = 0;
    public static final IntegerProperty ELAPSED_SECONDS = new IntegerProperty(
            TABLE, "elapsedSeconds");

    @ColumnInfo(name = "timerStart")
    public Long timerStart = 0L;
    public static final LongProperty TIMER_START = new LongProperty(
            TABLE, "timerStart");

    /** Flags for when to send reminders */
    @ColumnInfo(name = "notificationFlags")
    public Integer notificationFlags = 0;
    public static final IntegerProperty REMINDER_FLAGS = new IntegerProperty(
            TABLE, "notificationFlags");

    /** Reminder period, in milliseconds. 0 means disabled */
    @ColumnInfo(name = "notifications")
    public Long notifications = 0L;
    public static final LongProperty REMINDER_PERIOD = new LongProperty(
            TABLE, "notifications");

    /** Unixtime the last reminder was triggered */
    @ColumnInfo(name = "lastNotified")
    public Long lastNotified = 0L;
    public static final LongProperty REMINDER_LAST = new LongProperty(
            TABLE, "lastNotified");

    /** Unixtime snooze is set (0 -> no snooze) */
    @ColumnInfo(name = "snoozeTime")
    public Long snoozeTime = 0L;
    public static final LongProperty REMINDER_SNOOZE = new LongProperty(
            TABLE, "snoozeTime");

    @ColumnInfo(name = "recurrence")
    public String recurrence = "";
    public static final StringProperty RECURRENCE = new StringProperty(
            TABLE, "recurrence");

    @ColumnInfo(name = "repeatUntil")
    public Long repeatUntil = 0L;
    public static final LongProperty REPEAT_UNTIL = new LongProperty(
            TABLE, "repeatUntil");

    @ColumnInfo(name = "calendarUri")
    public String calendarUri = "";
    public static final StringProperty CALENDAR_URI = new StringProperty(
            TABLE, "calendarUri");

    // --- for astrid.com

    /** constant value for no uuid */
    public static final String NO_UUID = "0"; //$NON-NLS-1$

    /** Remote id */
    @ColumnInfo(name = "remoteId")
    public String remoteId = NO_UUID;
    public static final StringProperty UUID = new StringProperty(
            TABLE, "remoteId");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = new Property<?>[] {
            CALENDAR_URI,
            COMPLETION_DATE,
            CREATION_DATE,
            DELETION_DATE,
            DUE_DATE,
            ELAPSED_SECONDS,
            ESTIMATED_SECONDS,
            HIDE_UNTIL,
            ID,
            IMPORTANCE,
            MODIFICATION_DATE,
            NOTES,
            RECURRENCE,
            REMINDER_FLAGS,
            REMINDER_LAST,
            REMINDER_PERIOD,
            REMINDER_SNOOZE,
            REPEAT_UNTIL,
            TIMER_START,
            TITLE,
            UUID
    };

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

    public static final Map<String, ValueWriter<Object>> roomSetters = new HashMap<>();

    static {
        roomSetters.put(CALENDAR_URI.name, (t, v) -> t.calendarUri = (String) v);
        roomSetters.put(COMPLETION_DATE.name, (t, v) -> t.completed = (Long) v);
        roomSetters.put(CREATION_DATE.name, (t, v) -> t.created = (Long) v);
        roomSetters.put(DELETION_DATE.name, (t, v) -> t.deleted = (Long) v);
        roomSetters.put(DUE_DATE.name, (t, v) -> t.dueDate = v instanceof String ? Long.valueOf((String) v) : (Long) v);
        roomSetters.put(ELAPSED_SECONDS.name, (t, v) -> t.elapsedSeconds = (Integer) v);
        roomSetters.put(ESTIMATED_SECONDS.name, (t, v) -> t.estimatedSeconds = (Integer) v);
        roomSetters.put(HIDE_UNTIL.name, (t, v) -> t.hideUntil = (Long) v);
        roomSetters.put(ID.name, (t, v) -> t.id = (Long) v);
        roomSetters.put(IMPORTANCE.name, (t, v) -> t.importance = v instanceof String ? Integer.valueOf((String) v) : (Integer) v);
        roomSetters.put(MODIFICATION_DATE.name, (t, v) -> t.modified = (Long) v);
        roomSetters.put(NOTES.name, (t, v) -> t.notes = (String) v);
        roomSetters.put(RECURRENCE.name, (t, v) -> t.recurrence = (String) v);
        roomSetters.put(REMINDER_FLAGS.name, (t, v) -> t.notificationFlags = (Integer) v);
        roomSetters.put(REMINDER_LAST.name, (t, v) -> t.lastNotified = (Long) v);
        roomSetters.put(REMINDER_PERIOD.name, (t, v) -> t.notifications = (Long) v);
        roomSetters.put(REMINDER_SNOOZE.name, (t, v) -> t.snoozeTime = (Long) v);
        roomSetters.put(REPEAT_UNTIL.name, (t, v) -> t.repeatUntil = (Long) v);
        roomSetters.put(TIMER_START.name, (t, v) -> t.timerStart = (Long) v);
        roomSetters.put(TITLE.name, (t, v) -> t.title = (String) v);
        roomSetters.put(UUID.name, (t, v) -> t.remoteId = (String) v);
        roomSetters.put(GoogleTask.INDENT.name, (t, v) -> t.googleTaskIndent = (Integer) v);
    }

    @Ignore
    private int googleTaskIndent;

    // --- data access boilerplate

    public Task() {
        super();
    }

    @Ignore
    public Task(TodorooCursor cursor) {
        for (Property<?> property : cursor.getProperties()) {
            try {
                ValueWriter<?> writer = roomSetters.get(property.getColumnName());
                if (writer != null) {
                    writer.setValue(this, cursor.get(property));
                }
            } catch (IllegalArgumentException e) {
                // underlying cursor may have changed, suppress
                Timber.e(e, e.getMessage());
            }
        }
    }

    @Ignore
    public Task(XmlReader reader) {
        calendarUri = reader.readString("calendarUri");
        completed = reader.readLong("completed");
        created = reader.readLong("created");
        deleted = reader.readLong("deleted");
        dueDate = reader.readLong("dueDate");
        elapsedSeconds = reader.readInteger("elapsedSeconds");
        estimatedSeconds = reader.readInteger("estimatedSeconds");
        hideUntil = reader.readLong("hideUntil");
        importance = reader.readInteger("importance");
        modified = reader.readLong("modified");
        notes = reader.readString("notes");
        recurrence = reader.readString("recurrence");
        notificationFlags = reader.readInteger("notificationFlags");
        lastNotified = reader.readLong("lastNotified");
        notifications = reader.readLong("notifications");
        snoozeTime = reader.readLong("snoozeTime");
        repeatUntil = reader.readLong("repeatUntil");
        timerStart = reader.readLong("timerStart");
        title = reader.readString("title");
        remoteId = reader.readString("remoteId");
    }

    public void writeToXml(XmlWriter writer) {
        writer.writeString("calendarUri", calendarUri);
        writer.writeLong("completed", completed);
        writer.writeLong("created", created);
        writer.writeLong("deleted", deleted);
        writer.writeLong("dueDate", dueDate);
        writer.writeInteger("elapsedSeconds", elapsedSeconds);
        writer.writeInteger("estimatedSeconds", estimatedSeconds);
        writer.writeLong("hideUntil", hideUntil);
        writer.writeInteger("importance", importance);
        writer.writeLong("modified", modified);
        writer.writeString("notes", notes);
        writer.writeString("recurrence", recurrence);
        writer.writeInteger("notificationFlags", notificationFlags);
        writer.writeLong("lastNotified", lastNotified);
        writer.writeLong("notifications", notifications);
        writer.writeLong("snoozeTime", snoozeTime);
        writer.writeLong("repeatUntil", repeatUntil);
        writer.writeLong("timerStart", timerStart);
        writer.writeString("title", title);
        writer.writeString("remoteId", remoteId);
    }

    @Ignore
    public Task(Parcel parcel) {
        calendarUri = parcel.readString();
        completed = parcel.readLong();
        created = parcel.readLong();
        deleted = parcel.readLong();
        dueDate = parcel.readLong();
        elapsedSeconds = parcel.readInt();
        estimatedSeconds = parcel.readInt();
        hideUntil = parcel.readLong();
        id = parcel.readLong();
        importance = parcel.readInt();
        modified = parcel.readLong();
        notes = parcel.readString();
        recurrence = parcel.readString();
        notificationFlags = parcel.readInt();
        lastNotified = parcel.readLong();
        notifications = parcel.readLong();
        snoozeTime = parcel.readLong();
        repeatUntil = parcel.readLong();
        timerStart = parcel.readLong();
        title = parcel.readString();
        remoteId = parcel.readString();
        setValues.addAll(parcel.readArrayList(getClass().getClassLoader()));
        transitoryData = parcel.readHashMap(ContentValues.class.getClassLoader());
    }

    @Override
    public long getId() {
        return id == null ? NO_ID : id;
    }

    public String getUuid() {
        return Strings.isNullOrEmpty(remoteId) ? NO_UUID : remoteId;
    }

    // --- parcelable helpers

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel source) {
            return new Task(source);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

    // --- data access methods

    /** Checks whether task is done. Requires COMPLETION_DATE */
    public boolean isCompleted() {
        return completed > 0;
    }

    /** Checks whether task is deleted. Will return false if DELETION_DATE not read */
    public boolean isDeleted() {
        return deleted > 0;
    }

    /** Checks whether task is hidden. Requires HIDDEN_UNTIL */
    public boolean isHidden() {
        return hideUntil > DateUtilities.now();
    }

    public boolean hasHideUntilDate() {
        return hideUntil > 0;
    }

    /** Checks whether task is done. Requires DUE_DATE */
    public boolean hasDueDate() {
        return dueDate > 0;
    }

    // --- due and hide until date management

    /** urgency array index -> significance */
    public static final int URGENCY_NONE = 0;

    static final int URGENCY_TODAY = 1;
    static final int URGENCY_TOMORROW = 2;
    static final int URGENCY_DAY_AFTER = 3;
    static final int URGENCY_NEXT_WEEK = 4;
    static final int URGENCY_IN_TWO_WEEKS = 5;
    static final int URGENCY_NEXT_MONTH = 6;
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
            date = dueDate;
            break;
        case HIDE_UNTIL_DAY_BEFORE:
            date = dueDate - DateUtilities.ONE_DAY;
            break;
        case HIDE_UNTIL_WEEK_BEFORE:
            date = dueDate - DateUtilities.ONE_WEEK;
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
        return getRecurrenceWithoutFrom().replaceAll("BYDAY=;", "");  //$NON-NLS-1$//$NON-NLS-2$
    }

    public String getRecurrenceWithoutFrom() {
        return getRecurrence().replaceAll(";?FROM=[^;]*", "");
    }

    /**
     * Checks whether provided due date has a due time or only a date
     */
    public static boolean hasDueTime(long dueDate) {
        return dueDate > 0 && (dueDate % 60000 > 0);
    }

    public Long getDueDate() {
        return dueDate;
    }

    public void setDueDate(Long dueDate) {
        setValue(DUE_DATE, dueDate);
    }

    public void setDueDateAdjustingHideUntil(Long dueDate) {
        long oldDueDate = dueDate;
        if (oldDueDate > 0) {
            long hideUntil = this.hideUntil;
            if (hideUntil > 0) {
                setHideUntil(dueDate > 0 ? hideUntil + dueDate - oldDueDate : 0);
            }
        }
        setDueDate(dueDate);
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        setValue(RECURRENCE, recurrence);
    }

    public void setRecurrence(RRule rrule, boolean afterCompletion) {
        setRecurrence(rrule.toIcal() + (afterCompletion ? ";FROM=COMPLETION" : ""));
    }

    public Long getCreationDate() {
        return created;
    }

    public void setCreationDate(Long creationDate) {
        setValue(CREATION_DATE, creationDate);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        setValue(TITLE, title);
    }

    public Long getDeletionDate() {
        return deleted;
    }

    public void setDeletionDate(Long deletionDate) {
        setValue(DELETION_DATE, deletionDate);
    }

    public Long getHideUntil() {
        return hideUntil;
    }

    public void setHideUntil(Long hideUntil) {
        setValue(HIDE_UNTIL, hideUntil);
    }

    public Long getReminderLast() {
        return lastNotified;
    }

    public void setReminderLast(Long reminderLast) {
        setValue(REMINDER_LAST, reminderLast);
    }

    public Long getReminderSnooze() {
        return snoozeTime;
    }

    public void setReminderSnooze(Long reminderSnooze) {
        setValue(REMINDER_SNOOZE, reminderSnooze);
    }

    public Integer getElapsedSeconds() {
        return elapsedSeconds;
    }

    public Long getTimerStart() {
        return timerStart;
    }

    public void setTimerStart(Long timerStart) {
        setValue(TIMER_START, timerStart);
    }

    public Long getRepeatUntil() {
        return repeatUntil;
    }

    public void setRepeatUntil(Long repeatUntil) {
        setValue(REPEAT_UNTIL, repeatUntil);
    }

    public String getCalendarURI() {
        return calendarUri;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        setValue(IMPORTANCE, importance);
    }

    public Long getCompletionDate() {
        return completed;
    }

    public void setCompletionDate(Long completionDate) {
        setValue(COMPLETION_DATE, completionDate);
    }

    public String getNotes() {
        return notes;
    }

    public boolean hasNotes() {
        return !TextUtils.isEmpty(notes);
    }

    public void setNotes(String notes) {
        setValue(NOTES, notes);
    }

    public void setModificationDate(Long modificationDate) {
        setValue(MODIFICATION_DATE, modificationDate);
    }

    public Integer getReminderFlags() {
        return notificationFlags;
    }

    public void setReminderFlags(Integer reminderFlags) {
        setValue(REMINDER_FLAGS, reminderFlags);
    }

    public Long getReminderPeriod() {
        return notifications;
    }

    public void setReminderPeriod(Long reminderPeriod) {
        setValue(REMINDER_PERIOD, reminderPeriod);
    }

    public Integer getEstimatedSeconds() {
        return estimatedSeconds;
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
        return (notificationFlags & flag) > 0;
    }

    public boolean isNew() {
        return getId() == NO_ID;
    }

    public static boolean isValidUuid(String uuid) {
        try {
            long value = Long.parseLong(uuid);
            return value > 0;
        } catch (NumberFormatException e) {
            Timber.e(e, e.getMessage());
            return isUuidEmpty(uuid);
        }
    }

    public void setUuid(String uuid) {
        setValue(UUID, uuid);
    }

    public static boolean isUuidEmpty(String uuid) {
        return NO_UUID.equals(uuid) || TextUtils.isEmpty(uuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(calendarUri);
        dest.writeLong(completed);
        dest.writeLong(created);
        dest.writeLong(deleted);
        dest.writeLong(dueDate);
        dest.writeInt(elapsedSeconds);
        dest.writeInt(estimatedSeconds);
        dest.writeLong(hideUntil);
        dest.writeLong(id);
        dest.writeInt(importance);
        dest.writeLong(modified);
        dest.writeString(notes);
        dest.writeString(recurrence);
        dest.writeInt(notificationFlags);
        dest.writeLong(lastNotified);
        dest.writeLong(notifications);
        dest.writeLong(snoozeTime);
        dest.writeLong(repeatUntil);
        dest.writeLong(timerStart);
        dest.writeString(title);
        dest.writeString(remoteId);
        dest.writeList(newArrayList(setValues));
        dest.writeMap(transitoryData);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", importance=" + importance +
                ", setValues=" + setValues +
                ", dueDate=" + dueDate +
                ", transitoryData=" + transitoryData +
                ", hideUntil=" + hideUntil +
                ", created=" + created +
                ", modified=" + modified +
                ", completed=" + completed +
                ", deleted=" + deleted +
                ", notes='" + notes + '\'' +
                ", estimatedSeconds=" + estimatedSeconds +
                ", elapsedSeconds=" + elapsedSeconds +
                ", timerStart=" + timerStart +
                ", notificationFlags=" + notificationFlags +
                ", notifications=" + notifications +
                ", lastNotified=" + lastNotified +
                ", snoozeTime=" + snoozeTime +
                ", recurrence='" + recurrence + '\'' +
                ", repeatUntil=" + repeatUntil +
                ", calendarUri='" + calendarUri + '\'' +
                ", remoteId='" + remoteId + '\'' +
                '}';
    }

    public int getGoogleTaskIndent() {
        return googleTaskIndent;
    }
}
