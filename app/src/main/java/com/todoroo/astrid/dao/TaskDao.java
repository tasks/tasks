/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;

import org.tasks.BuildConfig;
import org.tasks.jobs.AfterSaveIntentService;
import org.tasks.receivers.PushReceiver;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public abstract int deleteById(long id);

    // --- save

    /**
     * Saves the given task to the database.getDatabase(). Task must already
     * exist. Returns true on success.
     *
     */
    public void save(Task task) {
        ContentValues modifiedValues = saveExisting(task);
        if (modifiedValues != null) {
            AfterSaveIntentService.enqueue(context, task.getId(), modifiedValues);
        } else if (task.checkTransitory(SyncFlags.FORCE_SYNC)) {
            PushReceiver.broadcast(context, task, null);
        }
    }

    @Insert
    abstract long insert(Task task);

    public void createNew(Task task) {
        task.id = null;
        task.remoteId = task.getUuid();
        task.setId(insert(task));
    }

    private ContentValues saveExisting(Task item) {
        ContentValues values = item.getSetValues();
        if (values == null || values.size() == 0) {
            return null;
        }
        if (!TaskApiDao.insignificantChange(values)) {
            if (!values.containsKey(Task.MODIFICATION_DATE.name)) {
                item.setModificationDate(now());
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
        if (updateAndRecordChanges(item, update)) {
            return values;
        }
        return null;
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

    public TodorooCursor fetchFiltered(String queryTemplate, Property<?>... properties) {
        return query(queryTemplate == null
                ? Query.selectDistinct(properties)
                : Query.select(properties).withQueryTemplate(PermaSql.replacePlaceholders(queryTemplate)));
    }

    /**
     * Construct a query with SQL DSL objects
     */
    public TodorooCursor query(Query query) {
        String queryString = query.from(Task.TABLE).toString();
        if (BuildConfig.DEBUG) {
            Timber.v(queryString);
        }
        Cursor cursor = database.rawQuery(queryString);
        return new TodorooCursor(cursor, query.getFields());
    }

    private interface DatabaseChangeOp {
        boolean makeChange();
    }

    private boolean updateAndRecordChanges(Task item, DatabaseChangeOp op) {
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

