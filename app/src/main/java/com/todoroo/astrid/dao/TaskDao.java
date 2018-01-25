/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.arch.persistence.room.Dao;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
import com.todoroo.astrid.helper.UUIDHelper;

import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.data.AlarmDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.LocationDao;
import org.tasks.data.TagDao;
import org.tasks.jobs.AfterSaveIntentService;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.PushReceiver;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Dao
public abstract class TaskDao {

    public static final String TRANS_SUPPRESS_REFRESH = "suppress-refresh";

    private final Database database;

    private LocalBroadcastManager localBroadcastManager;
    private Preferences preferences;
    private AlarmDao alarmDao;
    private TagDao tagDao;
    private LocationDao locationDao;
    private GoogleTaskDao googleTaskDao;
    private Context context;

    public TaskDao(Database database) {
        this.database = database;
    }

    public void initialize(Context context, Preferences preferences, LocalBroadcastManager localBroadcastManager,
                           AlarmDao alarmDao, TagDao tagDao, LocationDao locationDao, GoogleTaskDao googleTaskDao) {
        this.context = context;
        this.preferences = preferences;
        this.localBroadcastManager = localBroadcastManager;
        this.alarmDao = alarmDao;
        this.tagDao = tagDao;
        this.locationDao = locationDao;
        this.googleTaskDao = googleTaskDao;
    }

    public List<Task> selectActive(Criterion criterion) {
        return query(Query.select(Task.PROPERTIES).where(Criterion.and(TaskCriteria.isActive(), criterion))).toList();
    }

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
    public abstract Task fetch(long id);

    public int count(Filter filter) {
        String query = PermaSql.replacePlaceholders(filter.getSqlQuery());
        return count(Query.select(Task.ID).withQueryTemplate(query));
    }

    public int count(Query query) {
        Cursor cursor = query(query);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    public List<Task> query(Filter filter) {
        String query = PermaSql.replacePlaceholders(filter.getSqlQuery());
        return query(Query.select(Task.PROPERTIES).withQueryTemplate(query)).toList();
    }

    public List<Task> toList(Query query) {
        return query(query).toList();
    }

    @android.arch.persistence.room.Query("UPDATE tasks SET completed = :completionDate " +
            "WHERE remoteId = :remoteId")
    public abstract void setCompletionDate(String remoteId, long completionDate);

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class TaskCriteria {

    	/** @return tasks that were not deleted */
    	public static Criterion notDeleted() {
    	    return Task.DELETION_DATE.eq(0);
    	}

        public static Criterion notCompleted() {
            return Task.COMPLETION_DATE.eq(0);
        }

    	/** @return tasks that have not yet been completed or deleted */
    	public static Criterion activeAndVisible() {
            return Criterion.and(Task.COMPLETION_DATE.eq(0),
                    Task.DELETION_DATE.eq(0),
                    Task.HIDE_UNTIL.lt(Functions.now()));
    	}

    	/** @return tasks that have not yet been completed or deleted */
    	public static Criterion isActive() {
    	    return Criterion.and(Task.COMPLETION_DATE.eq(0),
    	            Task.DELETION_DATE.eq(0));
    	}

    	/** @return tasks that are not hidden at current time */
    	public static Criterion isVisible() {
    	    return Task.HIDE_UNTIL.lt(Functions.now());
        }

    	/** @return tasks that have a blank or null title */
        public static Criterion hasNoTitle() {
    	    return Criterion.or(Task.TITLE.isNull(), Task.TITLE.eq(""));
    	}
    }

    @android.arch.persistence.room.Query("SELECT remoteId FROM tasks WHERE _id = :localId")
    public abstract String uuidFromLocalId(long localId);

    @android.arch.persistence.room.Query("UPDATE tasks SET calendarUri = '' " +
            "WHERE calendarUri NOT NULL AND calendarUri != ''")
    public abstract void clearAllCalendarEvents();

    @android.arch.persistence.room.Query("UPDATE tasks SET calendarUri = '' " +
            "WHERE completed > 0 AND calendarUri NOT NULL AND calendarUri != ''")
    public abstract void clearCompletedCalendarEvents();

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE deleted > 0")
    public abstract List<Task> getDeleted();

    @android.arch.persistence.room.Query("DELETE FROM tasks WHERE _id = :id")
    abstract int deleteById(long id);

    // --- delete

    /**
     * Delete the given item
     *
     * @return true if delete was successful
     */
    public boolean delete(long id) {
        boolean result = deleteById(id) > 0;
        if(!result) {
            return false;
        }

        // delete all metadata
        alarmDao.deleteByTaskId(id);
        locationDao.deleteByTaskId(id);
        tagDao.deleteByTaskId(id);
        googleTaskDao.deleteByTaskId(id);

        localBroadcastManager.broadcastRefresh();

        return true;
    }

    // --- save

    /**
     * Saves the given task to the database.getDatabase(). Task must already
     * exist. Returns true on success.
     *
     */
    public void save(Task task) {
        ContentValues modifiedValues = createOrUpdate(task);
        if (modifiedValues != null) {
            AfterSaveIntentService.enqueue(context, task.getId(), modifiedValues);
        } else if (task.checkTransitory(SyncFlags.FORCE_SYNC)) {
            PushReceiver.broadcast(context, task, null);
        }
    }

    private ContentValues createOrUpdate(Task task) {
        return task.getId() == Task.NO_ID
                ? createNew(task)
                : saveExisting(task);
    }

    public ContentValues createNew(Task item) {
        if(!item.containsValue(Task.CREATION_DATE)) {
            item.setCreationDate(DateUtilities.now());
        }
        item.setModificationDate(DateUtilities.now());

        // set up task defaults
        if(!item.containsValue(Task.IMPORTANCE)) {
            item.setImportance(preferences.getIntegerFromString(
                    R.string.p_default_importance_key, Task.IMPORTANCE_SHOULD_DO));
        }
        if(!item.containsValue(Task.DUE_DATE)) {
            int setting = preferences.getIntegerFromString(R.string.p_default_urgency_key,
                    Task.URGENCY_NONE);
            item.setDueDate(Task.createDueDate(setting, 0));
        }
        createDefaultHideUntil(preferences, item);

        setDefaultReminders(preferences, item);

        ContentValues values = item.getSetValues();

        if (!item.containsValue(Task.UUID) || Task.isUuidEmpty(item.getUuid())) {
            item.setUuid(UUIDHelper.newUUID());
        }

        DatabaseChangeOp insert = new DatabaseChangeOp() {
            @Override
            public boolean makeChange() {
                ContentValues mergedValues = item.getMergedValues();
                mergedValues.remove(AbstractModel.ID_PROPERTY.name);
                long newRow = database.insert(mergedValues);
                boolean result = newRow >= 0;
                if (result) {
                    item.setId(newRow);
                }
                return result;
            }

            @Override
            public String toString() {
                return "INSERT";
            }
        };
        if (insertOrUpdateAndRecordChanges(item, insert)) {
            return values;
        }
        return null;
    }

    private static void createDefaultHideUntil(Preferences preferences, Task item) {
        if(!item.containsValue(Task.HIDE_UNTIL)) {
            int setting = preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                    Task.HIDE_UNTIL_NONE);
            item.setHideUntil(item.createHideUntil(setting, 0));
        }
    }

    /**
     * Sets default reminders for the given task if reminders are not set
     */
    public static void setDefaultReminders(Preferences preferences, Task item) {
        if(!item.containsValue(Task.REMINDER_PERIOD)) {
            item.setReminderPeriod(DateUtilities.ONE_HOUR *
                    preferences.getIntegerFromString(R.string.p_rmd_default_random_hours,
                            0));
        }
        if(!item.containsValue(Task.REMINDER_FLAGS)) {
            item.setReminderFlags(preferences.getDefaultReminders() | preferences.getDefaultRingMode());
        }
    }

    private ContentValues saveExisting(Task item) {
        ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0) {
            return null;
        }
        if(!TaskApiDao.insignificantChange(values)) {
            if(!values.containsKey(Task.MODIFICATION_DATE.name)) {
                item.setModificationDate(DateUtilities.now());
            }
        }
        DatabaseChangeOp update = new DatabaseChangeOp() {
            @Override
            public boolean makeChange() {
                return database.update(values,
                        AbstractModel.ID_PROPERTY.eq(item.getId()).toString()) > 0;
            }

            @Override
            public String toString() {
                return "UPDATE";
            }
        };
        if (insertOrUpdateAndRecordChanges(item, update)) {
            return values;
        }
        return null;
    }

    /**
     * Mark the given task as completed and save it.
     */
    public void setComplete(Task item, boolean completed) {
        if(completed) {
            item.setCompletionDate(DateUtilities.now());
        } else {
            item.setCompletionDate(0L);
        }

        save(item);
    }

    public TodorooCursor fetchFiltered(String queryTemplate, Property<?>... properties) {
        return query(queryTemplate == null
                ? Query.selectDistinct(properties)
                : Query.select(properties).withQueryTemplate(PermaSql.replacePlaceholders(queryTemplate)));
    }

    /**
     * Construct a query with SQL DSL objects
     */
    public TodorooCursor query(Query query) {
        query.from(Task.TABLE);
        String queryString = query.toString();
        if (BuildConfig.DEBUG) {
            Timber.v(queryString);
        }
        Cursor cursor = database.rawQuery(queryString);
        return new TodorooCursor(cursor, query.getFields());
    }

    private interface DatabaseChangeOp {
        boolean makeChange();
    }

    private boolean insertOrUpdateAndRecordChanges(Task item, DatabaseChangeOp op) {
        final AtomicBoolean result = new AtomicBoolean(false);
        synchronized(database) {
            result.set(op.makeChange());
            if (result.get()) {
                item.markSaved();
                if (BuildConfig.DEBUG) {
                    Timber.v("%s %s", op, item.toString());
                }
            }
        }
        return result.get();
    }
}

