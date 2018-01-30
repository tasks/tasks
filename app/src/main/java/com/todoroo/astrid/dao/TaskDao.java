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

import org.tasks.BuildConfig;
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

    private List<Task> toList(Cursor cursor) {
        List<Task> result = new ArrayList<>();
        try {
            for (cursor.moveToFirst() ; !cursor.isAfterLast() ; cursor.moveToNext()) {
                result.add(new Task(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public List<Task> query(Filter filter) {
        String query = PermaSql.replacePlaceholders(filter.getSqlQuery());
        return toList(query(Query.select().withQueryTemplate(query)));
    }

    public List<Task> toList(Query query) {
        return toList(query(query));
    }

    @android.arch.persistence.room.Query("UPDATE tasks SET completed = :completionDate " +
            "WHERE remoteId = :remoteId")
    public abstract void setCompletionDate(String remoteId, long completionDate);

    @android.arch.persistence.room.Query("UPDATE tasks SET snoozeTime = :millis WHERE _id in (:taskIds)")
    public abstract void snooze(List<Long> taskIds, long millis);

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

    @android.arch.persistence.room.Query("UPDATE tasks SET calendarUri = '' " +
            "WHERE calendarUri NOT NULL AND calendarUri != ''")
    public abstract void clearAllCalendarEvents();

    @android.arch.persistence.room.Query("UPDATE tasks SET calendarUri = '' " +
            "WHERE completed > 0 AND calendarUri NOT NULL AND calendarUri != ''")
    public abstract void clearCompletedCalendarEvents();

    @android.arch.persistence.room.Query("SELECT * FROM tasks WHERE deleted > 0")
    public abstract List<Task> getDeleted();

    @android.arch.persistence.room.Query("DELETE FROM tasks WHERE _id = :id")
    public abstract int deleteById(long id);

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
        task.remoteId = task.getUuid();
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

    public Cursor fetchFiltered(String queryTemplate, Property<?>... properties) {
        return query(Query.select(properties)
                .withQueryTemplate(PermaSql.replacePlaceholders(queryTemplate)));
    }

    public List<Task> fetchFiltered(String query) {
        return toList(fetchFiltered(query, Task.PROPERTIES));
    }

    /**
     * Construct a query with SQL DSL objects
     */
    public Cursor query(Query query) {
        String queryString = query.from(Task.TABLE).toString();
        if (BuildConfig.DEBUG) {
            Timber.v(queryString);
        }
        return database.rawQuery(queryString);
    }
}

