/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskDao extends RemoteModelDao<Task> {

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public TaskDao() {
        super(Task.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class TaskCriteria {

    	/** @returns tasks by id */
    	public static Criterion byId(long id) {
    	    return Task.ID.eq(id);
    	}

    	/** @return tasks that were deleted */
    	public static Criterion isDeleted() {
    	    return Task.DELETION_DATE.neq(0);
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

    	/** @return tasks that have not yet been completed or deleted and are assigned to me */
    	public static Criterion activeVisibleMine() {
    	    return Criterion.and(Task.COMPLETION_DATE.eq(0),
    	            Task.DELETION_DATE.eq(0),
    	            Task.HIDE_UNTIL.lt(Functions.now()),
    	            Task.IS_READONLY.eq(0),
    	            Task.USER_ID.eq(0));
    	}

    	/** @return tasks that have not yet been completed or deleted */
    	public static Criterion isActive() {
    	    return Criterion.and(Task.COMPLETION_DATE.eq(0),
    	            Task.DELETION_DATE.eq(0));
    	}

    	/** @return tasks that are due within the next 24 hours */
    	public static Criterion dueToday() {
    	    return Criterion.and(TaskCriteria.activeAndVisible(), Task.DUE_DATE.gt(0), Task.DUE_DATE.lt(Functions.fromNow(DateUtilities.ONE_DAY)));
    	}

    	/** @return tasks that are due within the next 72 hours */
    	public static Criterion dueSoon() {
    	    return Criterion.and(TaskCriteria.activeAndVisible(), Task.DUE_DATE.gt(0), Task.DUE_DATE.lt(Functions.fromNow(3 * DateUtilities.ONE_DAY)));
    	}

    	/** @return tasks that are not hidden at current time */
    	public static Criterion isVisible() {
    	    return Task.HIDE_UNTIL.lt(Functions.now());
        }

    	/** @return tasks that are hidden at the current time */
    	public static Criterion isHidden() {
    	    return Task.HIDE_UNTIL.gt(Functions.now());
    	}

    	/** @return tasks that have a due date */
    	public static Criterion hasDeadlines() {
    	    return Task.DUE_DATE.neq(0);
    	}

        /** @return tasks that are due before a certain unixtime */
        public static Criterion dueBeforeNow() {
            return Criterion.and(Task.DUE_DATE.gt(0), Task.DUE_DATE.lt(Functions.now()));
        }

        /** @return tasks that are due after a certain unixtime */
        public static Criterion dueAfterNow() {
            return Task.DUE_DATE.gt(Functions.now());
        }

    	/** @return tasks completed before a given unixtime */
    	public static Criterion completed() {
    	    return Criterion.and(Task.COMPLETION_DATE.gt(0), Task.COMPLETION_DATE.lt(Functions.now()));
    	}

    	/** @return tasks that have a blank or null title */
    	@SuppressWarnings("nls")
        public static Criterion hasNoTitle() {
    	    return Criterion.or(Task.TITLE.isNull(), Task.TITLE.eq(""));
    	}

    	/** Check if a given task belongs to someone else & is read-only */
        public static Criterion ownedByMe() {
             return Criterion.and(Task.IS_READONLY.eq(0),
                     Task.USER_ID.eq(0));
        }

    }

    // --- custom operations


    // --- delete

    /**
     * Delete the given item
     *
     * @param database
     * @param id
     * @return true if delete was successful
     */
    @Override
    public boolean delete(long id) {
        boolean result = super.delete(id);
        if(!result)
            return false;

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
     * @param task
     * @return true if save occurred, false otherwise (i.e. nothing changed)
     */
    public boolean save(Task task) {
        boolean saveSuccessful = false;
        if (task.getId() == Task.NO_ID) {
            try {
                saveSuccessful = createNew(task);
            } catch (SQLiteConstraintException e) {
                saveSuccessful = handleSQLiteConstraintException(task); // Tried to create task with remote id that already exists
            }
        } else {
            saveSuccessful = saveExisting(task);
        }

        return saveSuccessful;
    }

    public boolean handleSQLiteConstraintException(Task task) {
        TodorooCursor<Task> cursor = query(Query.select(Task.ID).where(
                Task.UUID.eq(task.getValue(Task.UUID))));
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            task.setId(cursor.get(Task.ID));
            return saveExisting(task);
        }
        return false;
    }

    @Override
    public boolean createNew(Task item) {
        if(!item.containsValue(Task.CREATION_DATE))
            item.setValue(Task.CREATION_DATE, DateUtilities.now());
        item.setValue(Task.MODIFICATION_DATE, DateUtilities.now());

        // set up task defaults
        if(!item.containsValue(Task.IMPORTANCE))
            item.setValue(Task.IMPORTANCE, Preferences.getIntegerFromString(
                    R.string.p_default_importance_key, Task.IMPORTANCE_SHOULD_DO));
        if(!item.containsValue(Task.DUE_DATE)) {
            int setting = Preferences.getIntegerFromString(R.string.p_default_urgency_key,
                    Task.URGENCY_NONE);
            item.setValue(Task.DUE_DATE, Task.createDueDate(setting, 0));
        }
        createDefaultHideUntil(item);

        setDefaultReminders(item);

        ContentValues values = item.getSetValues();
        boolean result = super.createNew(item);
        if(result) {
            afterSave(item, values);
        }

        return result;
    }

    public static void createDefaultHideUntil(Task item) {
        if(!item.containsValue(Task.HIDE_UNTIL)) {
            int setting = Preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                    Task.HIDE_UNTIL_NONE);
            item.setValue(Task.HIDE_UNTIL, item.createHideUntil(setting, 0));
        }
    }

    /**
     * Sets default reminders for the given task if reminders are not set
     * @param item
     */
    public static void setDefaultReminders(Task item) {
        if(!item.containsValue(Task.REMINDER_PERIOD)) {
            item.setValue(Task.REMINDER_PERIOD, DateUtilities.ONE_HOUR *
                    Preferences.getIntegerFromString(R.string.p_rmd_default_random_hours,
                            0));
        }
        if(!item.containsValue(Task.REMINDER_FLAGS)) {
            int reminder_flags = Preferences.getIntegerFromString(R.string.p_default_reminders_key,
                    Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE) |
                    Preferences.getIntegerFromString(R.string.p_default_reminders_mode_key, 0);
            item.setValue(Task.REMINDER_FLAGS, reminder_flags);
        }
    }

    @Override
    public boolean saveExisting(Task item) {
        ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0)
            return false;
        if(!TaskApiDao.insignificantChange(values)) {
            item.setValue(Task.DETAILS, null);
            if(!values.containsKey(Task.MODIFICATION_DATE.name))
                item.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }
        boolean result = super.saveExisting(item);
        if(result)
            afterSave(item, values);
        return result;
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

    @Override
    protected boolean shouldRecordOutstandingEntry(String columnName, Object value) {
        return NameMaps.shouldRecordOutstandingColumnForTable(NameMaps.TABLE_ID_TASKS, columnName);
    }

    public void saveExistingWithSqlConstraintCheck(Task item) {
        try {
            saveExisting(item);
        } catch (SQLiteConstraintException e) {
            String uuid = item.getValue(Task.UUID);
            TodorooCursor<Task> tasksWithUUID = query(Query.select(
                    SQL_CONSTRAINT_MERGE_PROPERTIES).where(
                    Task.UUID.eq(uuid)));
            try {
                if (tasksWithUUID.getCount() > 0) {
                    Task curr = new Task();
                    for (tasksWithUUID.moveToFirst();
                            !tasksWithUUID.isAfterLast(); tasksWithUUID.moveToNext()) {
                        curr.readFromCursor(tasksWithUUID);
                        if (curr.getId() == item.getId())
                            continue;

                        compareAndMergeAfterConflict(curr, fetch(item.getId(),
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
            if (p.equals(Task.ID))
                continue;
            if(existing.containsNonNullValue(p) != newConflict.containsNonNullValue(p))
                match = false;
            else if (existing.containsNonNullValue(p) &&
                    !existing.getValue(p).equals(newConflict.getValue(p)))
                match = false;
        }
        if (!match) {
            if (existing.getValue(Task.CREATION_DATE).equals(newConflict.getValue(Task.CREATION_DATE)))
                newConflict.setValue(Task.CREATION_DATE, newConflict.getValue(Task.CREATION_DATE) + 1000L);
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
    public static void afterSave(Task task, ContentValues values) {
        if(values == null)
            return;

        task.markSaved();
        if(values.containsKey(Task.COMPLETION_DATE.name) && task.isCompleted())
            afterComplete(task, values);
        else {
            if(values.containsKey(Task.DUE_DATE.name) ||
                    values.containsKey(Task.REMINDER_FLAGS.name) ||
                    values.containsKey(Task.REMINDER_PERIOD.name) ||
                    values.containsKey(Task.REMINDER_LAST.name) ||
                    values.containsKey(Task.REMINDER_SNOOZE.name))
                ReminderService.getInstance().scheduleAlarm(task);
        }

        // run api save hooks
        broadcastTaskSave(task, values);
    }

    /**
     * Send broadcasts on task change (triggers things like task repeats)
     * @param task task that was saved
     * @param values values that were updated
     */
    public static void broadcastTaskSave(Task task, ContentValues values) {
        if(TaskApiDao.insignificantChange(values))
            return;

        if(values.containsKey(Task.COMPLETION_DATE.name) && task.isCompleted()) {
            Context context = ContextManager.getContext();
            if(context != null) {
                Intent broadcastIntent;
                broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED);
                broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
                context.sendOrderedBroadcast(broadcastIntent, null);
            }
        }

        broadcastTaskChanged();
    }

    /**
     * Send broadcast when task list changes. Widgets should update.
     */
    public static void broadcastTaskChanged() {
        Context context = ContextManager.getContext();
        if(context != null) {
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_LIST_UPDATED);
            context.sendOrderedBroadcast(broadcastIntent, null);
        }
    }

    /**
     * Called after the task was just completed
     *
     * @param task
     * @param values
     */
    private static void afterComplete(Task task, ContentValues values) {
        Notifications.cancelNotifications(task.getId());
    }

}

