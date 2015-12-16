/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.location.GeofenceService;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class TaskDao {

    private final RemoteModelDao<Task> dao;

    private final MetadataDao metadataDao;
    private final Broadcaster broadcaster;
    private final ReminderService reminderService;
    private final NotificationManager notificationManager;
    private final Preferences preferences;
    private GeofenceService geofenceService;

    @Inject
	public TaskDao(Database database, MetadataDao metadataDao, Broadcaster broadcaster,
                   ReminderService reminderService, NotificationManager notificationManager,
                   Preferences preferences, GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
        dao = new RemoteModelDao<>(database, Task.class);
        this.preferences = preferences;
        this.metadataDao = metadataDao;
        this.broadcaster = broadcaster;
        this.reminderService = reminderService;
        this.notificationManager = notificationManager;
    }

    public TodorooCursor<Task> query(Query query) {
        return dao.query(query);
    }

    public Task fetch(long id, Property<?>... properties) {
        return dao.fetch(id, properties);
    }

    public int count(Query query) {
        return dao.count(query);
    }

    public TodorooCursor<Task> rawQuery(String selection, String[] selectionArgs, Property.LongProperty id) {
        return dao.rawQuery(selection, selectionArgs, id);
    }

    public int update(Criterion where, Task template) {
        return dao.update(where, template);
    }

    public int deleteWhere(Criterion criterion) {
        return dao.deleteWhere(criterion);
    }

    public void addListener(DatabaseDao.ModelUpdateListener<Task> modelUpdateListener) {
        dao.addListener(modelUpdateListener);
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

    	/** @return tasks by id */
    	public static Criterion byId(long id) {
    	    return Task.ID.eq(id);
    	}

    	/** @return tasks that were not deleted */
    	public static Criterion notDeleted() {
    	    return Task.DELETION_DATE.eq(0);
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
        metadataDao.deleteWhere(MetadataCriteria.byTask(id));

        broadcastTaskChanged();

        return true;
    }

    // --- save

    /**
     * Saves the given task to the database.getDatabase(). Task must already
     * exist. Returns true on success.
     *
     */
    public void save(Task task) {
        if (task.getId() == Task.NO_ID) {
            try {
                createNew(task);
            } catch (SQLiteConstraintException e) {
                Timber.e(e, e.getMessage());
                handleSQLiteConstraintException(task); // Tried to create task with remote id that already exists
            }
        } else {
            saveExisting(task);
        }
    }

    public void handleSQLiteConstraintException(Task task) {
        TodorooCursor<Task> cursor = dao.query(Query.select(Task.ID).where(
                Task.UUID.eq(task.getUUID())));
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            task.setId(cursor.get(Task.ID));
            saveExisting(task);
        }
    }

    public void createNew(Task item) {
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
            afterSave(item, values);
        }
    }

    public static void createDefaultHideUntil(Preferences preferences, Task item) {
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

    public void saveExisting(Task item) {
        ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0) {
            return;
        }
        if(!TaskApiDao.insignificantChange(values)) {
            if(!values.containsKey(Task.MODIFICATION_DATE.name)) {
                item.setModificationDate(DateUtilities.now());
            }
        }
        if(dao.saveExisting(item)) {
            afterSave(item, values);
        }
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
     * Called after the task is saved. This differs from the call in
     * TaskApiDao in that it runs hooks that need to be run from within
     * Astrid. Order matters here!
     */
    private void afterSave(Task task, ContentValues values) {
        if(values == null) {
            return;
        }

        task.markSaved();
        boolean completionDateModified = values.containsKey(Task.COMPLETION_DATE.name);
        boolean deletionDateModified = values.containsKey(Task.DELETION_DATE.name);
        if(completionDateModified && task.isCompleted()) {
            afterComplete(task);
        } else if (deletionDateModified && task.isDeleted()) {
            afterComplete(task);
        } else {
            if (completionDateModified || deletionDateModified) {
                geofenceService.setupGeofences(task.getId());
            }
            if(values.containsKey(Task.DUE_DATE.name) ||
                    values.containsKey(Task.REMINDER_FLAGS.name) ||
                    values.containsKey(Task.REMINDER_PERIOD.name) ||
                    values.containsKey(Task.REMINDER_LAST.name) ||
                    values.containsKey(Task.REMINDER_SNOOZE.name)) {
                reminderService.scheduleAlarm(this, task);
            }
        }

        // run api save hooks
        broadcastTaskSave(task, values);
    }

    /**
     * Send broadcasts on task change (triggers things like task repeats)
     * @param task task that was saved
     * @param values values that were updated
     */
    private void broadcastTaskSave(Task task, ContentValues values) {
        if(TaskApiDao.insignificantChange(values)) {
            return;
        }

        if(values.containsKey(Task.COMPLETION_DATE.name) && task.isCompleted()) {
            broadcaster.taskCompleted(task.getId());
        }

        broadcastTaskChanged();
    }

    /**
     * Send broadcast when task list changes. Widgets should update.
     */
    private void broadcastTaskChanged() {
        broadcaster.refresh();
    }

    /**
     * Called after the task was just completed
     */
    private void afterComplete(Task task) {
        long taskId = task.getId();
        notificationManager.cancel(taskId);
        geofenceService.cancelGeofences(taskId);
    }
}

