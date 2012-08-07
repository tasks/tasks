/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.LegacyAbstractModel;
import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.enums.RepeatInterval;


/** Abstract model of a task. Subclasses implement the getters and setters
 * they are interested in.
 *
 * @author timsu
 *
 */
@SuppressWarnings("nls")
public abstract class AbstractTaskModel extends LegacyAbstractModel {

    /** Version number of this model */
    static final int        VERSION                = 8;

    public static final int COMPLETE_PERCENTAGE    = 100;

    // field names

    public static final String     NAME                   = "name";
    public static final String     NOTES                  = "notes";
    public static final String     PROGRESS_PERCENTAGE    = "progressPercentage";
    public static final String     IMPORTANCE             = "importance";
    public static final String     ESTIMATED_SECONDS      = "estimatedSeconds";
    public static final String     ELAPSED_SECONDS        = "elapsedSeconds";
    public static final String     TIMER_START            = "timerStart";
    public static final String     DEFINITE_DUE_DATE      = "definiteDueDate";
    public static final String     PREFERRED_DUE_DATE     = "preferredDueDate";
    public static final String     HIDDEN_UNTIL           = "hiddenUntil";
    public static final String     POSTPONE_COUNT         = "postponeCount";
    public static final String     NOTIFICATIONS          = "notifications";
    public static final String     NOTIFICATION_FLAGS     = "notificationFlags";
    public static final String     LAST_NOTIFIED          = "lastNotified";
    public static final String     REPEAT                 = "repeat";
    public static final String     CREATION_DATE          = "creationDate";
    public static final String     COMPLETION_DATE        = "completionDate";
    public static final String     CALENDAR_URI           = "calendarUri";
    public static final String     FLAGS                  = "flags";

    // reserved fields ---
    public static final String     BLOCKING_ON            = "blockingOn";

    // notification flags
    public static final int NOTIFY_BEFORE_DEADLINE = 1 << 0;
    public static final int NOTIFY_AT_DEADLINE     = 1 << 1;
    public static final int NOTIFY_AFTER_DEADLINE  = 1 << 2;
    public static final int NOTIFY_NONSTOP         = 1 << 3;

    // other flags
    public static final int FLAG_SYNC_ON_COMPLETE  = 1 << 0;

    /** Number of bits to shift repeat value by */
    public static final int REPEAT_VALUE_OFFSET    = 3;

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(NAME, "");
        defaultValues.put(NOTES, "");
        defaultValues.put(PROGRESS_PERCENTAGE, 0);
        defaultValues.put(IMPORTANCE, Importance.DEFAULT.ordinal());
        defaultValues.put(ESTIMATED_SECONDS, 0);
        defaultValues.put(ELAPSED_SECONDS, 0);
        defaultValues.put(TIMER_START, 0);
        defaultValues.put(DEFINITE_DUE_DATE, 0);
        defaultValues.put(PREFERRED_DUE_DATE, 0);
        defaultValues.put(HIDDEN_UNTIL, 0);
        defaultValues.put(BLOCKING_ON, 0);
        defaultValues.put(POSTPONE_COUNT, 0);
        defaultValues.put(NOTIFICATIONS, 0);
        defaultValues.put(NOTIFICATION_FLAGS, NOTIFY_AT_DEADLINE);
        defaultValues.put(LAST_NOTIFIED, 0);
        defaultValues.put(REPEAT, 0);
        defaultValues.put(COMPLETION_DATE, 0);
        defaultValues.put(CALENDAR_URI, (String)null);
        defaultValues.put(FLAGS, 0);
    }

    // --- database helper

    /** Database Helper manages creating new tables and updating old ones */
    public static class TaskModelDatabaseHelper extends SQLiteOpenHelper {
        String tableName;
        Context context;

        public TaskModelDatabaseHelper(Context context, String databaseName, String tableName) {
            super(context, databaseName, null, VERSION);
            this.tableName = tableName;
            this.context = context;
        }

        @Override
        public synchronized void onCreate(SQLiteDatabase db) {
            String sql = new StringBuilder().
            append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (").
                append(LegacyAbstractController.KEY_ROWID).append(" integer primary key autoincrement, ").
                append(NAME).append(" text not null,").
                append(NOTES).append(" text not null,").
                append(PROGRESS_PERCENTAGE).append(" integer not null,").
                append(IMPORTANCE).append(" integer not null,").
                append(ESTIMATED_SECONDS).append(" integer,").
                append(ELAPSED_SECONDS).append(" integer,").
                append(TIMER_START).append(" integer,").
                append(DEFINITE_DUE_DATE).append(" integer,").
                append(PREFERRED_DUE_DATE).append(" integer,").
                append(HIDDEN_UNTIL).append(" integer,").
                append(BLOCKING_ON).append(" integer,").
                append(POSTPONE_COUNT).append(" integer,").
                append(NOTIFICATIONS).append(" integer,").
                append(NOTIFICATION_FLAGS).append(" integer,").
                append(LAST_NOTIFIED).append(" integer,").
                append(REPEAT).append(" integer,").
                append(FLAGS).append(" integer,").
                append(CREATION_DATE).append(" integer,").
                append(COMPLETION_DATE).append(" integer,").
                append(CALENDAR_URI).append(" text").
            append(");").toString();
            db.execSQL(sql);
        }

        @Override
        @SuppressWarnings("fallthrough")
        public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(getClass().getSimpleName(), "Upgrading database from version " +
                    oldVersion + " to " + newVersion + ".");
            String sql;

            // note: we execute sql statements in their own try block to be more
            // graceful if an upgrade dies halfway or something
            switch(oldVersion) {
            case 1:
                sql = new StringBuilder().append("ALTER TABLE ").
                    append(tableName).append(" ADD COLUMN ").
                    append(LAST_NOTIFIED).append(" integer").toString();
                try {
                    db.execSQL(sql);
                } catch (Exception e) {
                    Log.e("astrid", "Error updating table!", e);
                }
                sql = new StringBuilder().append("ALTER TABLE ").
                    append(tableName).append(" ADD COLUMN ").
                    append(NOTIFICATION_FLAGS).append(" integer").toString();
                try {
                    db.execSQL(sql);
                } catch (Exception e) {
                    Log.e("astrid", "Error updating table!", e);
                }

            case 2:
                sql = new StringBuilder().append("ALTER TABLE ").
                append(tableName).append(" ADD COLUMN ").
                append(REPEAT).append(" integer").toString();
                try {
                    db.execSQL(sql);
                } catch (Exception e) {
                    Log.e("astrid", "Error updating table!", e);
                }

            case 3:
                sql = new StringBuilder().append("ALTER TABLE ").
                append(tableName).append(" ADD COLUMN ").
                append(CALENDAR_URI).append(" text").toString();
                try {
                    db.execSQL(sql);
                } catch (Exception e) {
                    Log.e("astrid", "Error updating table!", e);
                }

            case 4:
                sql = new StringBuilder().append("ALTER TABLE ").
                append(tableName).append(" ADD COLUMN ").
                append(POSTPONE_COUNT).append(" integer").toString();
                try {
                    db.execSQL(sql);
                } catch (Exception e) {
                    Log.e("astrid", "Error updating table!", e);
                }

            case 5:
            case 6:
                // apparently some people didn't get the flags column
                // from version 5 to version 6, so we try again

                sql = new StringBuilder().append("ALTER TABLE ").
                append(tableName).append(" ADD COLUMN ").
                append(FLAGS).append(" integer").toString();
                try {
                    db.execSQL(sql);
                } catch (Exception e) {
                    Log.e("astrid", "Error updating table!", e);
                }

            case 7:
                // not a real change, but make sure that columns that are null
                // are converted into zeros, which was my previous assumption

                for(String column : new String[] {
                        ESTIMATED_SECONDS,
                        ELAPSED_SECONDS,
                        TIMER_START,
                        DEFINITE_DUE_DATE,
                        PREFERRED_DUE_DATE,
                        HIDDEN_UNTIL,
                        POSTPONE_COUNT,
                        LAST_NOTIFIED,
                        REPEAT,
                        CREATION_DATE,
                        COMPLETION_DATE }) {
                    sql = String.format("UPDATE %s SET %s = 0 WHERE %s ISNULL",
                            tableName, column, column);
                    try {
                        db.execSQL(sql);
                    } catch (Exception e) {
                        Log.e("astrid", "Error updating table!", e);
                    }
                }

                // --- break point

                break;

            default:
                // we don't know how to handle it... show an error
                Log.e(getClass().getSimpleName(), "Unsupported migration from " + oldVersion + " to " + newVersion);
            }
        }
    }

    // --- utility methods

    /** Gets task color. Requires definiteDueDate and importance
     * @param context */
    protected int getTaskColorResource(Context context) {
        if(getDefiniteDueDate() != null && getDefiniteDueDate().getTime() <
                System.currentTimeMillis()) {
            return R.color.task_list_overdue;
        } else {
            return R.color.task_list_normal;
        }
    }

    /** Checks whether task is done. Requires progressPercentage */
    protected boolean isTaskCompleted() {
        return getProgressPercentage() >= COMPLETE_PERCENTAGE;
    }

    /** Stops the timer & increments elapsed time. Requires timerStart and
     * elapsedSeconds */
    protected void stopTimerAndUpdateElapsedTime() {
        if(getTimerStart() == null)
            return;

        long start = getTimerStart().getTime();
        setTimerStart(null);
        long secondsElapsed = (System.currentTimeMillis() - start)/1000;
        setElapsedSeconds((int) (getElapsedSeconds() + secondsElapsed));
    }

    protected void prefetchData(String[] fields) {
        for(String field : fields) {
            if(field.equals(NAME))
                getName();
            else if(field.equals(NOTES))
                getNotes();
            else if(field.equals(PROGRESS_PERCENTAGE))
                getProgressPercentage();
            else if(field.equals(IMPORTANCE))
                getImportance();
            else if(field.equals(ESTIMATED_SECONDS))
                getEstimatedSeconds();
            else if(field.equals(ELAPSED_SECONDS))
                getElapsedSeconds();
            else if(field.equals(TIMER_START))
                getTimerStart();
            else if(field.equals(DEFINITE_DUE_DATE))
                getDefiniteDueDate();
            else if(field.equals(PREFERRED_DUE_DATE))
                getPreferredDueDate();
            else if(field.equals(HIDDEN_UNTIL))
                getHiddenUntil();
            else if(field.equals(BLOCKING_ON))
                getBlockingOn();
            else if(field.equals(POSTPONE_COUNT))
                getPostponeCount();
            else if(field.equals(NOTIFICATIONS))
                getNotificationIntervalSeconds();
            else if(field.equals(CREATION_DATE))
                getCreationDate();
            else if(field.equals(COMPLETION_DATE))
                getCompletionDate();
            else if(field.equals(NOTIFICATION_FLAGS))
                getNotificationFlags();
            else if(field.equals(LAST_NOTIFIED))
                getLastNotificationDate();
            else if(field.equals(REPEAT))
                getRepeat();
            else if(field.equals(FLAGS))
                getFlags();
        }
    }

    // --- helper classes

    public static class RepeatInfo {
        private final RepeatInterval interval;
        private final int value;

        public RepeatInfo(RepeatInterval repeatInterval, int value) {
            this.interval = repeatInterval;
            this.value = value;
        }

        public Date shiftDate(Date input) {
            Date newDate = (Date)input.clone();
            interval.offsetDateBy(newDate, value);
            return newDate;
        }

        public RepeatInterval getInterval() {
            return interval;
        }

        public int getValue() {
            return value;
        }

        public static int toSingleField(RepeatInfo repeatInfo) {
            int repeat;
            if(repeatInfo == null)
                repeat = 0;
            else
                repeat = (repeatInfo.value << REPEAT_VALUE_OFFSET) +
                    repeatInfo.interval.ordinal();
            return repeat;
        }

        public static RepeatInfo fromSingleField(int repeat) {
            if(repeat == 0)
                return null;
            int value = repeat >> REPEAT_VALUE_OFFSET;
            RepeatInterval interval = RepeatInterval.values()
                [repeat - (value << REPEAT_VALUE_OFFSET)];

            return new RepeatInfo(interval, value);
        }

    }

    // --- task identifier

    private TaskIdentifier identifier = null;

    public TaskIdentifier getTaskIdentifier() {
        return identifier;
    }

    void setTaskIdentifier(TaskIdentifier identifier) {
        this.identifier = identifier;
    }

    // --- constructors and abstract methods

    AbstractTaskModel() {
        super();
    }

    /** Read identifier from database */
    AbstractTaskModel(Cursor cursor) {
        super(cursor);

        Integer id = retrieveInteger(LegacyAbstractController.KEY_ROWID);
        setTaskIdentifier(new TaskIdentifier(id));
    }

    /** Get identifier from argument */
    AbstractTaskModel(TaskIdentifier identifier, Cursor cursor) {
        super(cursor);

        setTaskIdentifier(identifier);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- getters and setters: expose them as you see fit

    protected String getName() {
        return retrieveString(NAME);
    }

    protected String getNotes() {
        return retrieveString(NOTES);
    }

    protected int getProgressPercentage() {
        return retrieveInteger(PROGRESS_PERCENTAGE);
    }

    protected Importance getImportance() {
        Integer value = retrieveInteger(IMPORTANCE);
        if(value == null)
            return null;
        return Importance.values()[value];
    }

    protected Integer getEstimatedSeconds() {
        return retrieveInteger(ESTIMATED_SECONDS);
    }

    protected Integer getElapsedSeconds() {
        return retrieveInteger(ELAPSED_SECONDS);
    }

    protected Date getTimerStart() {
        return retrieveDate(TIMER_START);
    }

    protected Date getDefiniteDueDate() {
        return retrieveDate(DEFINITE_DUE_DATE);
    }

    protected Date getPreferredDueDate() {
        return retrieveDate(PREFERRED_DUE_DATE);
    }

    protected Date getHiddenUntil() {
        return retrieveDate(HIDDEN_UNTIL);
    }

    protected boolean isHidden() {
        if(getHiddenUntil() == null)
            return false;
        return getHiddenUntil().getTime() > System.currentTimeMillis();
    }

    protected Date getCreationDate() {
        return retrieveDate(CREATION_DATE);
    }

    protected Date getCompletionDate() {
        return retrieveDate(COMPLETION_DATE);
    }

    protected TaskIdentifier getBlockingOn() {
        Long value = retrieveLong(BLOCKING_ON);
        if(value == null)
            return null;
        return new TaskIdentifier(value);
    }

    protected Integer getPostponeCount() {
        return retrieveInteger(POSTPONE_COUNT);
    }

    protected Integer getNotificationIntervalSeconds() {
        return retrieveInteger(NOTIFICATIONS);
    }

    protected int getNotificationFlags() {
        return retrieveInteger(NOTIFICATION_FLAGS);
    }

    protected Date getLastNotificationDate() {
        return retrieveDate(LAST_NOTIFIED);
    }

    protected RepeatInfo getRepeat() {
        int repeat = retrieveInteger(REPEAT);
        if(repeat == 0)
            return null;
        int value = repeat >> REPEAT_VALUE_OFFSET;
        RepeatInterval interval = RepeatInterval.values()
            [repeat - (value << REPEAT_VALUE_OFFSET)];

        return new RepeatInfo(interval, value);
    }

    protected String getCalendarUri() {
        String uri = retrieveString(CALENDAR_URI);
        if(uri != null && uri.length() == 0)
            return null;
        else
            return uri;
    }

    protected int getFlags() {
        return retrieveInteger(FLAGS);
    }

    // --- setters

    protected void setName(String name) {
        putIfChangedFromDatabase(NAME, name);
    }

    protected void setNotes(String notes) {
        putIfChangedFromDatabase(NOTES, notes);
    }

    protected void setProgressPercentage(int progressPercentage) {
        putIfChangedFromDatabase(PROGRESS_PERCENTAGE, progressPercentage);

        if(getProgressPercentage() != progressPercentage &&
                progressPercentage == COMPLETE_PERCENTAGE)
            setCompletionDate(new Date());
    }

    protected void setImportance(Importance importance) {
        putIfChangedFromDatabase(IMPORTANCE, importance.ordinal());
    }

    protected void setEstimatedSeconds(Integer estimatedSeconds) {
        putIfChangedFromDatabase(ESTIMATED_SECONDS, estimatedSeconds);
    }

    protected void setElapsedSeconds(int elapsedSeconds) {
        putIfChangedFromDatabase(ELAPSED_SECONDS, elapsedSeconds);
    }

    protected void setTimerStart(Date timerStart) {
        putDate(TIMER_START, timerStart);
    }

    protected void setDefiniteDueDate(Date definiteDueDate) {
        putDate(DEFINITE_DUE_DATE, definiteDueDate);
    }

    protected void setPreferredDueDate(Date preferredDueDate) {
        putDate(PREFERRED_DUE_DATE, preferredDueDate);
    }

    protected void setHiddenUntil(Date hiddenUntil) {
        putDate(HIDDEN_UNTIL, hiddenUntil);
    }

    protected void setBlockingOn(TaskIdentifier blockingOn) {
        if(blockingOn == null || blockingOn.equals(getTaskIdentifier()))
            putIfChangedFromDatabase(BLOCKING_ON, (Integer)null);
        else
            putIfChangedFromDatabase(BLOCKING_ON, blockingOn.getId());
    }

    protected void setPostponeCount(int postponeCount) {
        putIfChangedFromDatabase(POSTPONE_COUNT, postponeCount);
    }

    protected void setCreationDate(Date creationDate) {
        putDate(CREATION_DATE, creationDate);
    }

    protected void setCompletionDate(Date completionDate) {
        putDate(COMPLETION_DATE, completionDate);
    }

    protected void setNotificationIntervalSeconds(Integer intervalInSeconds) {
        putIfChangedFromDatabase(NOTIFICATIONS, intervalInSeconds);
    }

    protected void setNotificationFlags(int flags) {
        putIfChangedFromDatabase(NOTIFICATION_FLAGS, flags);
    }

    protected void setLastNotificationTime(Date date) {
        putDate(LAST_NOTIFIED, date);
    }

    protected void setRepeat(RepeatInfo repeatInfo) {
        int repeat;
        if(repeatInfo == null)
            repeat = 0;
        else
            repeat = (repeatInfo.value << REPEAT_VALUE_OFFSET) +
                repeatInfo.interval.ordinal();
        putIfChangedFromDatabase(REPEAT, repeat);
    }

    protected void setCalendarUri(String uri) {
        putIfChangedFromDatabase(CALENDAR_URI, uri);
    }

    protected void setFlags(int flags) {
        putIfChangedFromDatabase(FLAGS, flags);
    }

    // --- utility methods

    protected void putDate(String fieldName, Date date) {
        if(date == null)
            putIfChangedFromDatabase(fieldName, 0);
        else
            putIfChangedFromDatabase(fieldName, date.getTime());
    }
}
