/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;

import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.data.AlarmDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.LocationDao;
import org.tasks.data.TagDao;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.jobs.AfterSaveIntentService;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.PushReceiver;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@ApplicationScope
public class TaskDao {

    public static final String TRANS_SUPPRESS_REFRESH = "suppress-refresh";

    private final RemoteModelDao<Task> dao;
    private final LocalBroadcastManager localBroadcastManager;

    private final Database database;
    private final Preferences preferences;
    private final AlarmDao alarmDao;
    private final TagDao tagDao;
    private final LocationDao locationDao;
    private final GoogleTaskDao googleTaskDao;
    private final Context context;

    @Inject
	public TaskDao(@ForApplication Context context, Database database,
                   Preferences preferences, LocalBroadcastManager localBroadcastManager,
                   AlarmDao alarmDao, TagDao tagDao, LocationDao locationDao, GoogleTaskDao googleTaskDao) {
        this.context = context;
        this.database = database;
        this.preferences = preferences;
        this.alarmDao = alarmDao;
        this.tagDao = tagDao;
        this.locationDao = locationDao;
        this.googleTaskDao = googleTaskDao;
        this.localBroadcastManager = localBroadcastManager;
        dao = new RemoteModelDao<>(database, Task.class);
    }

    public TodorooCursor<Task> query(Query query) {
        return dao.query(query);
    }

    public List<Task> selectActive(Criterion criterion) {
        return dao.toList(Query.select(Task.PROPERTIES).where(Criterion.and(TaskCriteria.isActive(), criterion)));
    }

    public Task fetch(long id) {
        return dao.fetch(id, Task.PROPERTIES);
    }

    public int count(Filter filter) {
        String query = PermaSql.replacePlaceholders(filter.getSqlQuery());
        return count(Query.select(Task.ID).withQueryTemplate(query));
    }

    public int count(Query query) {
        return dao.count(query);
    }

    public List<Task> query(Filter filter) {
        String query = PermaSql.replacePlaceholders(filter.getSqlQuery());
        return dao.toList(Query.select(Task.PROPERTIES).withQueryTemplate(query));
    }

    /**
     * Update all matching a clause to have the values set on template object.
     * <p>
     * Example (updates "joe" => "bob" in metadata value1):
     * {code}
     * Metadata item = new Metadata();
     * item.setVALUE1("bob");
     * update(item, Metadata.VALUE1.eq("joe"));
     * {code}
     * @param where sql criteria
     * @param template set fields on this object in order to set them in the db.
     * @return # of updated items
     */
    public int update(Criterion where, Task template) {
        return dao.update(where, template);
    }

    public int deleteWhere(Criterion criterion) {
        return dao.deleteWhere(criterion);
    }

    public List<Task> toList(Query query) {
        return dao.toList(query);
    }

    public void persist(Task task) {
        dao.persist(task);
    }

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

    public String uuidFromLocalId(long localId) {
        TodorooCursor<Task> cursor = dao.query(Query.select(RemoteModel.UUID_PROPERTY).where(AbstractModel.ID_PROPERTY.eq(localId)));
        try {
            if (cursor.getCount() == 0) {
                return RemoteModel.NO_UUID;
            }
            cursor.moveToFirst();
            return cursor.get(RemoteModel.UUID_PROPERTY);
        } finally {
            cursor.close();
        }
    }

    // --- delete

    /**
     * Delete the given item
     *
     * @return true if delete was successful
     */
    public boolean delete(long id) {
        boolean result = dao.delete(id);
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
        if (task.getId() == Task.NO_ID) {
            try {
                return createNew(task);
            } catch (SQLiteConstraintException e) {
                Timber.e(e, e.getMessage());
                return handleSQLiteConstraintException(task); // Tried to create task with remote id that already exists
            }
        } else {
            return saveExisting(task);
        }
    }

    private ContentValues handleSQLiteConstraintException(Task task) {
        TodorooCursor<Task> cursor = dao.query(Query.select(Task.ID).where(
                Task.UUID.eq(task.getUUID())));
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            task.setId(cursor.get(Task.ID));
            return saveExisting(task);
        }
        return null;
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
        if(dao.createNew(item)) {
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

    public ContentValues saveExisting(Task item) {
        ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0) {
            return null;
        }
        if(!TaskApiDao.insignificantChange(values)) {
            if(!values.containsKey(Task.MODIFICATION_DATE.name)) {
                item.setModificationDate(DateUtilities.now());
            }
        }
        if(dao.saveExisting(item)) {
            return values;
        }
        return null;
    }

    private static final Property<?>[] SQL_CONSTRAINT_MERGE_PROPERTIES = new Property<?>[] {
        Task.ID,
        Task.UUID,
        Task.TITLE,
        Task.IMPORTANCE,
        Task.DUE_DATE,
        Task.CREATION_DATE,
        Task.DELETION_DATE,
        Task.NOTES,
        Task.HIDE_UNTIL,
        Task.RECURRENCE
    };

    public void saveExistingWithSqlConstraintCheck(Task item) {
        try {
            saveExisting(item);
        } catch (SQLiteConstraintException e) {
            Timber.e(e, e.getMessage());
            String uuid = item.getUUID();
            TodorooCursor<Task> tasksWithUUID = dao.query(Query.select(
                    SQL_CONSTRAINT_MERGE_PROPERTIES).where(
                    Task.UUID.eq(uuid)));
            try {
                if (tasksWithUUID.getCount() > 0) {
                    for (tasksWithUUID.moveToFirst(); !tasksWithUUID.isAfterLast(); tasksWithUUID.moveToNext()) {
                        Task curr = new Task(tasksWithUUID);
                        if (curr.getId() == item.getId()) {
                            continue;
                        }

                        compareAndMergeAfterConflict(curr, dao.fetch(item.getId(),
                                tasksWithUUID.getProperties()));
                        return;
                    }
                } else {
                    // We probably want to know about this case, because
                    // it means that the constraint error isn't caused by
                    // UUID
                    throw e;
                }
            } finally {
                tasksWithUUID.close();
            }
        }
    }

    private void compareAndMergeAfterConflict(Task existing, Task newConflict) {
        boolean match = true;
        for (Property<?> p : SQL_CONSTRAINT_MERGE_PROPERTIES) {
            if (p.equals(Task.ID)) {
                continue;
            }
            if(existing.containsNonNullValue(p) != newConflict.containsNonNullValue(p)) {
                match = false;
            } else if (existing.containsNonNullValue(p) &&
                    !existing.getValue(p).equals(newConflict.getValue(p))) {
                match = false;
            }
        }
        if (!match) {
            if (existing.getCreationDate().equals(newConflict.getCreationDate())) {
                newConflict.setCreationDate(newConflict.getCreationDate() + 1000L);
            }
            newConflict.clearValue(Task.UUID);
            saveExisting(newConflict);
        } else {
            delete(newConflict.getId());
        }
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

    public TodorooCursor<Task> fetchFiltered(String queryTemplate, Property<?>... properties) {
        return query(queryTemplate == null
                ? Query.selectDistinct(properties)
                : Query.select(properties).withQueryTemplate(PermaSql.replacePlaceholders(queryTemplate)));
    }
}

