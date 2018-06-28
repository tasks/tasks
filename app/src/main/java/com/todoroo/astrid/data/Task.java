/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static org.tasks.date.DateTimeUtils.newDateTime;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import com.google.common.base.Strings;
import com.google.ical.values.RRule;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.DateUtilities;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.HashMap;
import org.tasks.backup.XmlReader;
import org.tasks.data.Tag;
import org.tasks.time.DateTime;
import timber.log.Timber;

/**
 * Data Model which represents a task users need to accomplish.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@Entity(tableName = "tasks", indices = @Index(name = "t_rid", value = "remoteId", unique = true))
public class Task implements Parcelable {

  // --- table and uri

  /** table for this model */
  public static final Table TABLE = new Table("tasks");

  public static final long NO_ID = 0;

  // --- properties
  public static final LongProperty ID = new LongProperty(TABLE, "_id");
  public static final StringProperty TITLE = new StringProperty(TABLE, "title");
  public static final IntegerProperty IMPORTANCE = new IntegerProperty(TABLE, "importance");
  public static final LongProperty DUE_DATE = new LongProperty(TABLE, "dueDate");
  public static final LongProperty HIDE_UNTIL = new LongProperty(TABLE, "hideUntil");
  public static final LongProperty MODIFICATION_DATE = new LongProperty(TABLE, "modified");
  public static final LongProperty COMPLETION_DATE = new LongProperty(TABLE, "completed");
  public static final LongProperty DELETION_DATE = new LongProperty(TABLE, "deleted");
  public static final StringProperty NOTES = new StringProperty(TABLE, "notes");
  public static final LongProperty TIMER_START = new LongProperty(TABLE, "timerStart");
  /** constant value for no uuid */
  public static final String NO_UUID = "0"; // $NON-NLS-1$

  public static final StringProperty UUID = new StringProperty(TABLE, "remoteId");
  /** List of all properties for this model */
  public static final Property<?>[] PROPERTIES =
      new Property<?>[] {
        new StringProperty(TABLE, "calendarUri"),
        COMPLETION_DATE,
        new LongProperty(TABLE, "created"),
        DELETION_DATE,
        DUE_DATE,
        new IntegerProperty(TABLE, "elapsedSeconds"),
        new IntegerProperty(TABLE, "estimatedSeconds"),
        HIDE_UNTIL,
        ID,
        IMPORTANCE,
        MODIFICATION_DATE,
        NOTES,
        new StringProperty(TABLE, "recurrence"),
        new IntegerProperty(TABLE, "notificationFlags"),
        new LongProperty(TABLE, "lastNotified"),
        new LongProperty(TABLE, "notifications"),
        new LongProperty(TABLE, "snoozeTime"),
        new LongProperty(TABLE, "repeatUntil"),
        TIMER_START,
        TITLE,
        UUID
      };
  /** whether to send a reminder at deadline */
  public static final int NOTIFY_AT_DEADLINE = 1 << 1;
  /** whether to send reminders while task is overdue */
  public static final int NOTIFY_AFTER_DEADLINE = 1 << 2;
  /** reminder mode non-stop */
  public static final int NOTIFY_MODE_NONSTOP = 1 << 3;
  /** reminder mode five times (exclusive with non-stop) */
  public static final int NOTIFY_MODE_FIVE = 1 << 4;

  public static final Creator<Task> CREATOR =
      new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel source) {
          return new Task(source);
        }

        @Override
        public Task[] newArray(int size) {
          return new Task[size];
        }
      };
  /** urgency array index -> significance */
  public static final int URGENCY_NONE = 0;

  public static final int URGENCY_SPECIFIC_DAY = 7;
  public static final int URGENCY_SPECIFIC_DAY_TIME = 8;
  /** hide until array index -> significance */
  public static final int HIDE_UNTIL_NONE = 0;

  public static final int HIDE_UNTIL_DUE = 1;
  public static final int HIDE_UNTIL_DAY_BEFORE = 2;
  public static final int HIDE_UNTIL_WEEK_BEFORE = 3;
  public static final int HIDE_UNTIL_SPECIFIC_DAY = 4;
  // --- for astrid.com
  public static final int HIDE_UNTIL_SPECIFIC_DAY_TIME = 5;
  public static final int HIDE_UNTIL_DUE_TIME = 6;
  static final int URGENCY_TODAY = 1;
  static final int URGENCY_TOMORROW = 2;
  // --- notification flags
  static final int URGENCY_DAY_AFTER = 3;
  static final int URGENCY_NEXT_WEEK = 4;
  static final int URGENCY_IN_TWO_WEEKS = 5;
  static final int URGENCY_NEXT_MONTH = 6;
  /** ID */
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  public transient Long id = NO_ID;

  // --- importance settings (note: importance > 3 are supported via plugin)
  /** Name of Task */
  @ColumnInfo(name = "title")
  public String title = "";

  @ColumnInfo(name = "importance")
  public Integer priority = Priority.NONE;
  /** Unixtime Task is due, 0 if not set */
  @ColumnInfo(name = "dueDate")
  public Long dueDate = 0L;
  /** Unixtime Task should be hidden until, 0 if not set */
  @ColumnInfo(name = "hideUntil")
  public Long hideUntil = 0L;
  /** Unixtime Task was created */
  @ColumnInfo(name = "created")
  public Long created = 0L;
  /** Unixtime Task was last touched */
  @ColumnInfo(name = "modified")
  public Long modified = 0L;
  /** Unixtime Task was completed. 0 means active */
  @ColumnInfo(name = "completed")
  public Long completed = 0L;
  /** Unixtime Task was deleted. 0 means not deleted */
  @ColumnInfo(name = "deleted")
  public Long deleted = 0L;

  // --- data access boilerplate
  @ColumnInfo(name = "notes")
  public String notes = "";

  @ColumnInfo(name = "estimatedSeconds")
  public Integer estimatedSeconds = 0;

  @ColumnInfo(name = "elapsedSeconds")
  public Integer elapsedSeconds = 0;

  @ColumnInfo(name = "timerStart")
  public Long timerStart = 0L;
  /** Flags for when to send reminders */
  @ColumnInfo(name = "notificationFlags")
  public Integer notificationFlags = 0;
  /** Reminder period, in milliseconds. 0 means disabled */
  @ColumnInfo(name = "notifications")
  public Long notifications = 0L;

  // --- parcelable helpers
  /** Unixtime the last reminder was triggered */
  @ColumnInfo(name = "lastNotified")
  public Long lastNotified = 0L;

  // --- data access methods
  /** Unixtime snooze is set (0 -> no snooze) */
  @ColumnInfo(name = "snoozeTime")
  public Long snoozeTime = 0L;

  @ColumnInfo(name = "recurrence")
  public String recurrence = "";

  @ColumnInfo(name = "repeatUntil")
  public Long repeatUntil = 0L;

  @ColumnInfo(name = "calendarUri")
  public String calendarUri = "";
  /** Remote id */
  @ColumnInfo(name = "remoteId")
  public String remoteId = NO_UUID;

  // --- due and hide until date management
  @Ignore private transient int indent;
  @Ignore private transient String tags;
  @Ignore private transient String googleTaskList;
  @Ignore private transient String caldav;
  @Ignore private transient boolean hasFiles;
  @Ignore private transient HashMap<String, Object> transitoryData = null;

  public Task() {}

  @Ignore
  public Task(Cursor _cursor) {
    final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("_id");
    final int _cursorIndexOfTitle = _cursor.getColumnIndexOrThrow("title");
    final int _cursorIndexOfImportance = _cursor.getColumnIndexOrThrow("importance");
    final int _cursorIndexOfDueDate = _cursor.getColumnIndexOrThrow("dueDate");
    final int _cursorIndexOfHideUntil = _cursor.getColumnIndexOrThrow("hideUntil");
    final int _cursorIndexOfCreated = _cursor.getColumnIndexOrThrow("created");
    final int _cursorIndexOfModified = _cursor.getColumnIndexOrThrow("modified");
    final int _cursorIndexOfCompleted = _cursor.getColumnIndexOrThrow("completed");
    final int _cursorIndexOfDeleted = _cursor.getColumnIndexOrThrow("deleted");
    final int _cursorIndexOfNotes = _cursor.getColumnIndexOrThrow("notes");
    final int _cursorIndexOfEstimatedSeconds = _cursor.getColumnIndexOrThrow("estimatedSeconds");
    final int _cursorIndexOfElapsedSeconds = _cursor.getColumnIndexOrThrow("elapsedSeconds");
    final int _cursorIndexOfTimerStart = _cursor.getColumnIndexOrThrow("timerStart");
    final int _cursorIndexOfNotificationFlags = _cursor.getColumnIndexOrThrow("notificationFlags");
    final int _cursorIndexOfNotifications = _cursor.getColumnIndexOrThrow("notifications");
    final int _cursorIndexOfLastNotified = _cursor.getColumnIndexOrThrow("lastNotified");
    final int _cursorIndexOfSnoozeTime = _cursor.getColumnIndexOrThrow("snoozeTime");
    final int _cursorIndexOfRecurrence = _cursor.getColumnIndexOrThrow("recurrence");
    final int _cursorIndexOfRepeatUntil = _cursor.getColumnIndexOrThrow("repeatUntil");
    final int _cursorIndexOfCalendarUri = _cursor.getColumnIndexOrThrow("calendarUri");
    final int _cursorIndexOfRemoteId = _cursor.getColumnIndexOrThrow("remoteId");
    final int _cursorIndexOfIndent = _cursor.getColumnIndex("indent");
    final int _cursorIndexOfTags = _cursor.getColumnIndex("tags");
    final int _cursorIndexOfGoogleTasks = _cursor.getColumnIndex("googletask");
    final int _cursorIndexOfCaldav = _cursor.getColumnIndex("caldav");
    final int _cursorIndexOfFileId = _cursor.getColumnIndex("fileId");
    if (_cursor.isNull(_cursorIndexOfId)) {
      id = null;
    } else {
      id = _cursor.getLong(_cursorIndexOfId);
    }
    title = _cursor.getString(_cursorIndexOfTitle);
    if (_cursor.isNull(_cursorIndexOfImportance)) {
      priority = null;
    } else {
      priority = _cursor.getInt(_cursorIndexOfImportance);
    }
    if (_cursor.isNull(_cursorIndexOfDueDate)) {
      dueDate = null;
    } else {
      dueDate = _cursor.getLong(_cursorIndexOfDueDate);
    }
    if (_cursor.isNull(_cursorIndexOfHideUntil)) {
      hideUntil = null;
    } else {
      hideUntil = _cursor.getLong(_cursorIndexOfHideUntil);
    }
    if (_cursor.isNull(_cursorIndexOfCreated)) {
      created = null;
    } else {
      created = _cursor.getLong(_cursorIndexOfCreated);
    }
    if (_cursor.isNull(_cursorIndexOfModified)) {
      modified = null;
    } else {
      modified = _cursor.getLong(_cursorIndexOfModified);
    }
    if (_cursor.isNull(_cursorIndexOfCompleted)) {
      completed = null;
    } else {
      completed = _cursor.getLong(_cursorIndexOfCompleted);
    }
    if (_cursor.isNull(_cursorIndexOfDeleted)) {
      deleted = null;
    } else {
      deleted = _cursor.getLong(_cursorIndexOfDeleted);
    }
    notes = _cursor.getString(_cursorIndexOfNotes);
    if (_cursor.isNull(_cursorIndexOfEstimatedSeconds)) {
      estimatedSeconds = null;
    } else {
      estimatedSeconds = _cursor.getInt(_cursorIndexOfEstimatedSeconds);
    }
    if (_cursor.isNull(_cursorIndexOfElapsedSeconds)) {
      elapsedSeconds = null;
    } else {
      elapsedSeconds = _cursor.getInt(_cursorIndexOfElapsedSeconds);
    }
    if (_cursor.isNull(_cursorIndexOfTimerStart)) {
      timerStart = null;
    } else {
      timerStart = _cursor.getLong(_cursorIndexOfTimerStart);
    }
    if (_cursor.isNull(_cursorIndexOfNotificationFlags)) {
      notificationFlags = null;
    } else {
      notificationFlags = _cursor.getInt(_cursorIndexOfNotificationFlags);
    }
    if (_cursor.isNull(_cursorIndexOfNotifications)) {
      notifications = null;
    } else {
      notifications = _cursor.getLong(_cursorIndexOfNotifications);
    }
    if (_cursor.isNull(_cursorIndexOfLastNotified)) {
      lastNotified = null;
    } else {
      lastNotified = _cursor.getLong(_cursorIndexOfLastNotified);
    }
    if (_cursor.isNull(_cursorIndexOfSnoozeTime)) {
      snoozeTime = null;
    } else {
      snoozeTime = _cursor.getLong(_cursorIndexOfSnoozeTime);
    }
    recurrence = _cursor.getString(_cursorIndexOfRecurrence);
    if (_cursor.isNull(_cursorIndexOfRepeatUntil)) {
      repeatUntil = null;
    } else {
      repeatUntil = _cursor.getLong(_cursorIndexOfRepeatUntil);
    }
    calendarUri = _cursor.getString(_cursorIndexOfCalendarUri);
    remoteId = _cursor.getString(_cursorIndexOfRemoteId);
    if (_cursorIndexOfIndent >= 0) {
      indent = _cursor.getInt(_cursorIndexOfIndent);
    }
    if (_cursorIndexOfTags >= 0) {
      tags = _cursor.getString(_cursorIndexOfTags);
    }
    if (_cursorIndexOfGoogleTasks >= 0) {
      googleTaskList = _cursor.getString(_cursorIndexOfGoogleTasks);
    }
    if (_cursorIndexOfCaldav >= 0) {
      caldav = _cursor.getString(_cursorIndexOfCaldav);
    }
    if (_cursorIndexOfFileId >= 0) {
      hasFiles = _cursor.getInt(_cursorIndexOfFileId) > 0;
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
    priority = reader.readInteger("importance");
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
    priority = parcel.readInt();
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
    transitoryData = parcel.readHashMap(ContentValues.class.getClassLoader());
    indent = parcel.readInt();
  }

  /**
   * Creates due date for this task. If this due date has no time associated, we move it to the last
   * millisecond of the day.
   *
   * @param setting one of the URGENCY_* constants
   * @param customDate if specific day or day & time is set, this value
   */
  public static long createDueDate(int setting, long customDate) {
    long date;

    switch (setting) {
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

    if (date <= 0) {
      return date;
    }

    DateTime dueDate = newDateTime(date).withMillisOfSecond(0);
    if (setting != URGENCY_SPECIFIC_DAY_TIME) {
      dueDate =
          dueDate
              .withHourOfDay(12)
              .withMinuteOfHour(0)
              .withSecondOfMinute(0); // Seconds == 0 means no due time
    } else {
      dueDate = dueDate.withSecondOfMinute(1); // Seconds > 0 means due time exists
    }
    return dueDate.getMillis();
  }

  /** Checks whether provided due date has a due time or only a date */
  public static boolean hasDueTime(long dueDate) {
    return dueDate > 0 && (dueDate % 60000 > 0);
  }

  public static boolean isValidUuid(String uuid) {
    try {
      long value = Long.parseLong(uuid);
      return value > 0;
    } catch (NumberFormatException e) {
      Timber.e(e);
      return isUuidEmpty(uuid);
    }
  }

  public static boolean isUuidEmpty(String uuid) {
    return NO_UUID.equals(uuid) || TextUtils.isEmpty(uuid);
  }

  public long getId() {
    return id == null ? NO_ID : id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUuid() {
    return Strings.isNullOrEmpty(remoteId) ? NO_UUID : remoteId;
  }

  public void setUuid(String uuid) {
    remoteId = uuid;
  }

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

  /**
   * Create hide until for this task.
   *
   * @param setting one of the HIDE_UNTIL_* constants
   * @param customDate if specific day is set, this value
   */
  public long createHideUntil(int setting, long customDate) {
    long date;

    switch (setting) {
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

    if (date <= 0) {
      return date;
    }

    DateTime hideUntil = newDateTime(date).withMillisOfSecond(0); // get rid of millis
    if (setting != HIDE_UNTIL_SPECIFIC_DAY_TIME && setting != HIDE_UNTIL_DUE_TIME) {
      hideUntil = hideUntil.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
    } else {
      hideUntil = hideUntil.withSecondOfMinute(1);
    }
    return hideUntil.getMillis();
  }

  /** Checks whether this due date has a due time or only a date */
  public boolean hasDueTime() {
    return hasDueDate() && hasDueTime(getDueDate());
  }

  public boolean isOverdue() {
    long dueDate = getDueDate();
    long compareTo =
        hasDueTime() ? DateUtilities.now() : DateUtilities.getStartOfDay(DateUtilities.now());

    return dueDate < compareTo && !isCompleted();
  }

  public boolean repeatAfterCompletion() {
    return getRecurrence().contains("FROM=COMPLETION");
  }

  public String sanitizedRecurrence() {
    return getRecurrenceWithoutFrom().replaceAll("BYDAY=;", ""); // $NON-NLS-1$//$NON-NLS-2$
  }

  public String getRecurrenceWithoutFrom() {
    return getRecurrence().replaceAll(";?FROM=[^;]*", "");
  }

  public Long getDueDate() {
    return dueDate;
  }

  public void setDueDate(Long dueDate) {
    this.dueDate = dueDate;
  }

  public void setDueDateAdjustingHideUntil(Long newDueDate) {
    if (dueDate > 0) {
      if (hideUntil > 0) {
        setHideUntil(newDueDate > 0 ? hideUntil + newDueDate - dueDate : 0);
      }
    }
    setDueDate(newDueDate);
  }

  public boolean isRecurring() {
    return !Strings.isNullOrEmpty(recurrence);
  }

  public String getRecurrence() {
    return recurrence;
  }

  public void setRecurrence(String recurrence) {
    this.recurrence = recurrence;
  }

  public void setRecurrence(RRule rrule, boolean afterCompletion) {
    setRecurrence(rrule.toIcal() + (afterCompletion ? ";FROM=COMPLETION" : ""));
  }

  public Long getCreationDate() {
    return created;
  }

  public void setCreationDate(Long creationDate) {
    created = creationDate;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Long getDeletionDate() {
    return deleted;
  }

  public void setDeletionDate(Long deletionDate) {
    deleted = deletionDate;
  }

  public Long getHideUntil() {
    return hideUntil;
  }

  public void setHideUntil(Long hideUntil) {
    this.hideUntil = hideUntil;
  }

  public Long getReminderLast() {
    return lastNotified;
  }

  public void setReminderLast(Long reminderLast) {
    lastNotified = reminderLast;
  }

  public Long getReminderSnooze() {
    return snoozeTime;
  }

  public void setReminderSnooze(Long reminderSnooze) {
    snoozeTime = reminderSnooze;
  }

  public Integer getElapsedSeconds() {
    return elapsedSeconds;
  }

  public void setElapsedSeconds(Integer elapsedSeconds) {
    this.elapsedSeconds = elapsedSeconds;
  }

  public Long getTimerStart() {
    return timerStart;
  }

  public void setTimerStart(Long timerStart) {
    this.timerStart = timerStart;
  }

  public Long getRepeatUntil() {
    return repeatUntil;
  }

  public void setRepeatUntil(Long repeatUntil) {
    this.repeatUntil = repeatUntil;
  }

  public String getCalendarURI() {
    return calendarUri;
  }

  public @Priority Integer getPriority() {
    return priority;
  }

  public void setPriority(@Priority Integer priority) {
    this.priority = priority;
  }

  public Long getCompletionDate() {
    return completed;
  }

  public void setCompletionDate(Long completionDate) {
    completed = completionDate;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public boolean hasNotes() {
    return !TextUtils.isEmpty(notes);
  }

  public Long getModificationDate() {
    return modified;
  }

  public void setModificationDate(Long modificationDate) {
    modified = modificationDate;
  }

  public Integer getReminderFlags() {
    return notificationFlags;
  }

  public void setReminderFlags(Integer reminderFlags) {
    notificationFlags = reminderFlags;
  }

  public Long getReminderPeriod() {
    return notifications;
  }

  public void setReminderPeriod(Long reminderPeriod) {
    notifications = reminderPeriod;
  }

  public Integer getEstimatedSeconds() {
    return estimatedSeconds;
  }

  public void setEstimatedSeconds(Integer estimatedSeconds) {
    this.estimatedSeconds = estimatedSeconds;
  }

  public void setCalendarUri(String calendarUri) {
    this.calendarUri = calendarUri;
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

  /** {@inheritDoc} */
  @Override
  public int describeContents() {
    return 0;
  }

  /** {@inheritDoc} */
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
    dest.writeInt(priority);
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
    dest.writeMap(transitoryData);
    dest.writeInt(indent);
  }

  @Override
  public String toString() {
    return "Task{"
        + "id="
        + id
        + ", title='"
        + title
        + '\''
        + ", priority="
        + priority
        + ", dueDate="
        + dueDate
        + ", transitoryData="
        + transitoryData
        + ", hideUntil="
        + hideUntil
        + ", created="
        + created
        + ", modified="
        + modified
        + ", completed="
        + completed
        + ", deleted="
        + deleted
        + ", notes='"
        + notes
        + '\''
        + ", estimatedSeconds="
        + estimatedSeconds
        + ", elapsedSeconds="
        + elapsedSeconds
        + ", timerStart="
        + timerStart
        + ", notificationFlags="
        + notificationFlags
        + ", notifications="
        + notifications
        + ", lastNotified="
        + lastNotified
        + ", snoozeTime="
        + snoozeTime
        + ", recurrence='"
        + recurrence
        + '\''
        + ", repeatUntil="
        + repeatUntil
        + ", calendarUri='"
        + calendarUri
        + '\''
        + ", remoteId='"
        + remoteId
        + '\''
        + '}';
  }

  public int getIndent() {
    return indent;
  }

  public void setIndent(int indent) {
    this.indent = indent;
  }

  public boolean insignificantChange(Task task) {
    if (this == task) {
      return true;
    }
    if (task == null) {
      return false;
    }

    if (id != null ? !id.equals(task.id) : task.id != null) {
      return false;
    }
    if (title != null ? !title.equals(task.title) : task.title != null) {
      return false;
    }
    if (priority != null ? !priority.equals(task.priority) : task.priority != null) {
      return false;
    }
    if (dueDate != null ? !dueDate.equals(task.dueDate) : task.dueDate != null) {
      return false;
    }
    if (hideUntil != null ? !hideUntil.equals(task.hideUntil) : task.hideUntil != null) {
      return false;
    }
    if (created != null ? !created.equals(task.created) : task.created != null) {
      return false;
    }
    if (modified != null ? !modified.equals(task.modified) : task.modified != null) {
      return false;
    }
    if (completed != null ? !completed.equals(task.completed) : task.completed != null) {
      return false;
    }
    if (deleted != null ? !deleted.equals(task.deleted) : task.deleted != null) {
      return false;
    }
    if (notes != null ? !notes.equals(task.notes) : task.notes != null) {
      return false;
    }
    if (estimatedSeconds != null
        ? !estimatedSeconds.equals(task.estimatedSeconds)
        : task.estimatedSeconds != null) {
      return false;
    }
    if (elapsedSeconds != null
        ? !elapsedSeconds.equals(task.elapsedSeconds)
        : task.elapsedSeconds != null) {
      return false;
    }
    if (notificationFlags != null
        ? !notificationFlags.equals(task.notificationFlags)
        : task.notificationFlags != null) {
      return false;
    }
    if (notifications != null
        ? !notifications.equals(task.notifications)
        : task.notifications != null) {
      return false;
    }
    if (recurrence != null ? !recurrence.equals(task.recurrence) : task.recurrence != null) {
      return false;
    }
    if (repeatUntil != null ? !repeatUntil.equals(task.repeatUntil) : task.repeatUntil != null) {
      return false;
    }
    if (calendarUri != null ? !calendarUri.equals(task.calendarUri) : task.calendarUri != null) {
      return false;
    }
    return remoteId != null ? remoteId.equals(task.remoteId) : task.remoteId == null;
  }

  public boolean googleTaskUpToDate(Task original) {
    if (this == original) {
      return true;
    }
    if (original == null) {
      return false;
    }

    if (title != null ? !title.equals(original.title) : original.title != null) {
      return false;
    }
    if (dueDate != null ? !dueDate.equals(original.dueDate) : original.dueDate != null) {
      return false;
    }
    if (completed != null ? !completed.equals(original.completed) : original.completed != null) {
      return false;
    }
    if (deleted != null ? !deleted.equals(original.deleted) : original.deleted != null) {
      return false;
    }
    return notes != null ? notes.equals(original.notes) : original.notes == null;
  }

  public boolean caldavUpToDate(Task original) {
    if (this == original) {
      return true;
    }
    if (original == null) {
      return false;
    }

    if (title != null ? !title.equals(original.title) : original.title != null) {
      return false;
    }
    if (priority != null ? !priority.equals(original.priority) : original.priority != null) {
      return false;
    }
    if (dueDate != null ? !dueDate.equals(original.dueDate) : original.dueDate != null) {
      return false;
    }
    if (completed != null ? !completed.equals(original.completed) : original.completed != null) {
      return false;
    }
    if (deleted != null ? !deleted.equals(original.deleted) : original.deleted != null) {
      return false;
    }
    if (notes != null ? !notes.equals(original.notes) : original.notes != null) {
      return false;
    }
    if (recurrence != null
        ? !recurrence.equals(original.recurrence)
        : original.recurrence != null) {
      return false;
    }
    return repeatUntil != null
        ? repeatUntil.equals(original.repeatUntil)
        : original.repeatUntil == null;
  }

  public boolean isSaved() {
    return getId() != NO_ID;
  }

  public synchronized void putTransitory(String key, Object value) {
    if (transitoryData == null) {
      transitoryData = new HashMap<>();
    }
    transitoryData.put(key, value);
  }

  public ArrayList<String> getTags() {
    Object tags = getTransitory(Tag.KEY);
    return tags == null ? new ArrayList<>() : (ArrayList<String>) tags;
  }

  public void setTags(ArrayList<String> tags) {
    if (transitoryData == null) {
      transitoryData = new HashMap<>();
    }
    transitoryData.put(Tag.KEY, tags);
  }

  public boolean hasTransitory(String key) {
    return transitoryData != null && transitoryData.containsKey(key);
  }

  public <T> T getTransitory(String key) {
    if (transitoryData == null) {
      return null;
    }
    return (T) transitoryData.get(key);
  }

  private Object clearTransitory(String key) {
    if (transitoryData == null) {
      return null;
    }
    return transitoryData.remove(key);
  }

  // --- Convenience wrappers for using transitories as flags
  public boolean checkTransitory(String flag) {
    Object trans = getTransitory(flag);
    return trans != null;
  }

  public boolean checkAndClearTransitory(String flag) {
    Object trans = clearTransitory(flag);
    return trans != null;
  }

  public String getTagsString() {
    return tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Task)) {
      return false;
    }

    Task task = (Task) o;

    if (indent != task.indent) {
      return false;
    }
    if (hasFiles != task.hasFiles) {
      return false;
    }
    if (id != null ? !id.equals(task.id) : task.id != null) {
      return false;
    }
    if (title != null ? !title.equals(task.title) : task.title != null) {
      return false;
    }
    if (priority != null ? !priority.equals(task.priority) : task.priority != null) {
      return false;
    }
    if (dueDate != null ? !dueDate.equals(task.dueDate) : task.dueDate != null) {
      return false;
    }
    if (hideUntil != null ? !hideUntil.equals(task.hideUntil) : task.hideUntil != null) {
      return false;
    }
    if (created != null ? !created.equals(task.created) : task.created != null) {
      return false;
    }
    if (modified != null ? !modified.equals(task.modified) : task.modified != null) {
      return false;
    }
    if (completed != null ? !completed.equals(task.completed) : task.completed != null) {
      return false;
    }
    if (deleted != null ? !deleted.equals(task.deleted) : task.deleted != null) {
      return false;
    }
    if (notes != null ? !notes.equals(task.notes) : task.notes != null) {
      return false;
    }
    if (estimatedSeconds != null
        ? !estimatedSeconds.equals(task.estimatedSeconds)
        : task.estimatedSeconds != null) {
      return false;
    }
    if (elapsedSeconds != null
        ? !elapsedSeconds.equals(task.elapsedSeconds)
        : task.elapsedSeconds != null) {
      return false;
    }
    if (timerStart != null ? !timerStart.equals(task.timerStart) : task.timerStart != null) {
      return false;
    }
    if (notificationFlags != null
        ? !notificationFlags.equals(task.notificationFlags)
        : task.notificationFlags != null) {
      return false;
    }
    if (notifications != null
        ? !notifications.equals(task.notifications)
        : task.notifications != null) {
      return false;
    }
    if (lastNotified != null
        ? !lastNotified.equals(task.lastNotified)
        : task.lastNotified != null) {
      return false;
    }
    if (snoozeTime != null ? !snoozeTime.equals(task.snoozeTime) : task.snoozeTime != null) {
      return false;
    }
    if (recurrence != null ? !recurrence.equals(task.recurrence) : task.recurrence != null) {
      return false;
    }
    if (repeatUntil != null ? !repeatUntil.equals(task.repeatUntil) : task.repeatUntil != null) {
      return false;
    }
    if (calendarUri != null ? !calendarUri.equals(task.calendarUri) : task.calendarUri != null) {
      return false;
    }
    if (remoteId != null ? !remoteId.equals(task.remoteId) : task.remoteId != null) {
      return false;
    }
    if (tags != null ? !tags.equals(task.tags) : task.tags != null) {
      return false;
    }
    if (googleTaskList != null
        ? !googleTaskList.equals(task.googleTaskList)
        : task.googleTaskList != null) {
      return false;
    }
    return caldav != null ? caldav.equals(task.caldav) : task.caldav == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (priority != null ? priority.hashCode() : 0);
    result = 31 * result + (dueDate != null ? dueDate.hashCode() : 0);
    result = 31 * result + (hideUntil != null ? hideUntil.hashCode() : 0);
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (modified != null ? modified.hashCode() : 0);
    result = 31 * result + (completed != null ? completed.hashCode() : 0);
    result = 31 * result + (deleted != null ? deleted.hashCode() : 0);
    result = 31 * result + (notes != null ? notes.hashCode() : 0);
    result = 31 * result + (estimatedSeconds != null ? estimatedSeconds.hashCode() : 0);
    result = 31 * result + (elapsedSeconds != null ? elapsedSeconds.hashCode() : 0);
    result = 31 * result + (timerStart != null ? timerStart.hashCode() : 0);
    result = 31 * result + (notificationFlags != null ? notificationFlags.hashCode() : 0);
    result = 31 * result + (notifications != null ? notifications.hashCode() : 0);
    result = 31 * result + (lastNotified != null ? lastNotified.hashCode() : 0);
    result = 31 * result + (snoozeTime != null ? snoozeTime.hashCode() : 0);
    result = 31 * result + (recurrence != null ? recurrence.hashCode() : 0);
    result = 31 * result + (repeatUntil != null ? repeatUntil.hashCode() : 0);
    result = 31 * result + (calendarUri != null ? calendarUri.hashCode() : 0);
    result = 31 * result + (remoteId != null ? remoteId.hashCode() : 0);
    result = 31 * result + indent;
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    result = 31 * result + (googleTaskList != null ? googleTaskList.hashCode() : 0);
    result = 31 * result + (caldav != null ? caldav.hashCode() : 0);
    result = 31 * result + (hasFiles ? 1 : 0);
    return result;
  }

  public String getGoogleTaskList() {
    return googleTaskList;
  }

  public String getCaldav() {
    return caldav;
  }

  public boolean hasFiles() {
    return hasFiles;
  }

  @Retention(SOURCE)
  @IntDef({Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.NONE})
  public @interface Priority {
    int HIGH = 0;
    int MEDIUM = 1;
    int LOW = 2;
    int NONE = 3;
  }
}
