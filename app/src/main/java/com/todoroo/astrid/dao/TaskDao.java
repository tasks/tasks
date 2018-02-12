/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Update;
import android.content.Context;
import android.database.Cursor;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;

import org.tasks.BuildConfig;
import org.tasks.data.LimitOffsetDataSource;
import org.tasks.jobs.AfterSaveIntentService;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.todoroo.andlib.utility.DateUtilities.now;

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

    private Context context;

    public TaskDao(Database database) {
        this.database = database;
    }

    public void initialize(Context context) {
        this.context = context;
    }

    public List<Task> needsRefresh() {
        return needsRefresh(now());
    }

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0 AND (hideUntil > :now OR dueDate > :now)")
    abstract List<Task> needsRefresh(long now);

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE _id = :id LIMIT 1")
    public abstract Task fetch(long id);

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE _id IN (:taskIds)")
    public abstract List<Task> fetch(List<Long> taskIds);

    @android.arch.persistence.room.Query("SELECT COUNT(1) FROM tasks WHERE timerStart > 0")
    public abstract int activeTimers();

    @android.arch.persistence.room.Query("SELECT tasks.* FROM tasks INNER JOIN notification ON tasks._id = notification.task")
    public abstract List<Task> activeNotifications();

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE remoteId = :remoteId")
    public abstract Task fetch(String remoteId);

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE completed = 0 AND deleted = 0")
    abstract List<Task> getActiveTasks();

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE hideUntil < (strftime('%s','now')*1000)")
    abstract List<Task> getVisibleTasks();

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE remoteId IN (:remoteIds) " +
            "AND recurrence NOT NULL AND LENGTH(recurrence) > 0")
    public abstract List<Task> getRecurringTasks(List<String> remoteIds);

    @android.arch.persistence.room.Query("UPDATE tasks SET completed = :completionDate " +
            "WHERE remoteId = :remoteId")
    public abstract void setCompletionDate(String remoteId, long completionDate);

    @android.arch.persistence.room.Query("UPDATE tasks SET snoozeTime = :millis WHERE _id in (:taskIds)")
    public abstract void snooze(List<Long> taskIds, long millis);

    @android.arch.persistence.room.Query("SELECT tasks.* FROM tasks " +
            "LEFT JOIN google_tasks ON tasks._id = google_tasks.task " +
            "WHERE tasks.modified > google_tasks.last_sync " +
            "OR google_tasks.remote_id = '' " +
            "OR google_tasks.remote_id IS NULL")
    public abstract List<Task> getTasksToPush();

    @android.arch.persistence.room.Query("SELECT * FROM TASKS " +
            "WHERE completed = 0 AND deleted = 0 AND (notificationFlags > 0 OR notifications > 0)")
    public abstract List<Task> getTasksWithReminders();

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
    }

    @android.arch.persistence.room.Query("SELECT * FROM tasks")
    public abstract List<Task> getAll();

    @android.arch.persistence.room.Query("SELECT calendarUri FROM tasks " +
            "WHERE calendarUri NOT NULL AND calendarUri != ''")
    public abstract List<String> getAllCalendarEvents();

    @android.arch.persistence.room.Query("UPDATE tasks SET calendarUri = '' " +
            "WHERE calendarUri NOT NULL AND calendarUri != ''")
    public abstract int clearAllCalendarEvents();

    @android.arch.persistence.room.Query("SELECT calendarUri FROM tasks " +
            "WHERE completed > 0 AND calendarUri NOT NULL AND calendarUri != ''")
    public abstract List<String> getCompletedCalendarEvents();

    @android.arch.persistence.room.Query("UPDATE tasks SET calendarUri = '' " +
            "WHERE completed > 0 AND calendarUri NOT NULL AND calendarUri != ''")
    public abstract int clearCompletedCalendarEvents();

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE deleted > 0")
    public abstract List<Task> getDeleted();

    @android.arch.persistence.room.Query("DELETE FROM tasks WHERE _id = :id")
    public abstract int deleteById(long id);

    @android.arch.persistence.room.Query("SELECT tasks.* FROM tasks INNER JOIN google_tasks ON google_tasks.task = tasks._id WHERE google_tasks.list_id = :googleTaskList")
    public abstract List<Task> getGoogleTasks(String googleTaskList);

    // --- save

    /**
     * Saves the given task to the database.getDatabase(). Task must already
     * exist. Returns true on success.
     *
     */
    public void save(Task task) {
        save(task, fetch(task.getId()));
    }

    // TODO: get rid of this super-hack
    public void save(Task task, Task original) {
        if (saveExisting(task, original)) {
            AfterSaveIntentService.enqueue(context, task.getId(), original);
        }
    }

    @Insert
    abstract long insert(Task task);

    @Update
    abstract int update(Task task);

    public void createNew(Task task) {
        task.id = null;
        if (task.created == 0) {
            task.created = now();
        }
        if (Task.isUuidEmpty(task.remoteId)) {
            task.remoteId = UUIDHelper.newUUID();
        }
        long insert = insert(task);
        task.setId(insert);
    }

    private boolean saveExisting(Task item, Task original) {
        if (!item.insignificantChange(original)) {
            item.setModificationDate(now());
        }
        int updated = update(item);
        if (updated == 1) {
            database.onDatabaseUpdated();
            return true;
        }
        return false;
    }

    @android.arch.persistence.room.Query("SELECT * FROM tasks " +
            "WHERE completed = 0 AND deleted = 0 AND hideUntil < (strftime('%s','now')*1000) " +
            "ORDER BY (CASE WHEN (dueDate=0) THEN (strftime('%s','now')*1000)*2 ELSE ((CASE WHEN (dueDate / 60000) > 0 THEN dueDate ELSE (dueDate + 43140000) END)) END) + 172800000 * importance ASC " +
            "LIMIT 100")
    public abstract List<Task> getAstrid2TaskProviderTasks();

    /**
     * Mark the given task as completed and save it.
     */
    public void setComplete(Task item, boolean completed) {
        if(completed) {
            item.setCompletionDate(now());
        } else {
            item.setCompletionDate(0L);
        }

        save(item);
    }

    public int count(Filter filter) {
        String query = Query
                .select(new Property.CountProperty())
                .withQueryTemplate(PermaSql.replacePlaceholders(filter.getSqlQuery()))
                .from(Task.TABLE)
                .toString();
        Cursor cursor = database.rawQuery(query);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    public List<Task> fetchFiltered(Filter filter) {
        return fetchFiltered(filter.getSqlQuery());
    }

    public List<Task> fetchFiltered(String queryTemplate) {
        Cursor cursor = getCursor(queryTemplate);
        List<Task> result = new ArrayList<>();
        try {
            for (cursor.moveToFirst() ; !cursor.isAfterLast() ; cursor.moveToNext()) {
                result.add(new Task(cursor));
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    public Cursor getCursor(String queryTemplate) {
        Query query = Query.select(Task.PROPERTIES).withQueryTemplate(PermaSql.replacePlaceholders(queryTemplate));
        String queryString = query.from(Task.TABLE).toString();
        if (BuildConfig.DEBUG) {
            Timber.v(queryString);
        }
        return database.rawQuery(queryString);
    }

    public LimitOffsetDataSource getLimitOffsetDataSource(String queryTemplate, Property<?>... properties) {
        String query = Query
                .select(properties)
                .withQueryTemplate(PermaSql.replacePlaceholders(queryTemplate))
                .from(Task.TABLE).toString();
        return new LimitOffsetDataSource(database, query);
    }
}

