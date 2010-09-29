/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.widget.PowerWidget;
import com.todoroo.astrid.widget.PowerWidget42;
import com.todoroo.astrid.widget.TasksWidget;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskDao extends DatabaseDao<Task> {

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

    	/** @return tasks that have not yet been completed or deleted */
    	public static Criterion isActive() {
    	    return Criterion.and(Task.COMPLETION_DATE.eq(0),
    	            Task.DELETION_DATE.eq(0));
    	}

    	/** @return tasks that are not hidden at current time */
    	public static Criterion isVisible() {
    	    return Task.HIDE_UNTIL.lt(Functions.now());
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

        afterTasklistChange();

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
        boolean saveSuccessful;

        ContentValues values = task.getSetValues();
        if(values == null || values.size() == 0) {
            if(task.getDatabaseValues() != null)
                return false;
        }

        if (task.getId() == Task.NO_ID) {
            saveSuccessful = createNew(task);
        } else {
            saveSuccessful = saveExisting(task);
        }

        if(saveSuccessful) {
            task.markSaved();
            afterSave(task, values);
        }

        return saveSuccessful;
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
            item.setValue(Task.DUE_DATE, item.createDueDate(setting, 0));
        }
        if(!item.containsValue(Task.HIDE_UNTIL)) {
            int setting = Preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                    Task.HIDE_UNTIL_NONE);
            item.setValue(Task.HIDE_UNTIL, item.createHideUntil(setting, 0));
        }
        if(!item.containsValue(Task.REMINDER_PERIOD)) {
            item.setValue(Task.REMINDER_PERIOD, DateUtilities.ONE_HOUR *
                    Preferences.getIntegerFromString(R.string.p_rmd_default_random_hours,
                            0));
        }
        if(!item.containsValue(Task.REMINDER_FLAGS)) {
            item.setValue(Task.REMINDER_FLAGS,
                    Preferences.getIntegerFromString(R.string.p_default_reminders_key,
                            Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE));
        }

        return super.createNew(item);
    }

    @Override
    public boolean saveExisting(Task item) {
        if(!item.getSetValues().containsKey(Task.DETAILS_DATE.name)) {
            item.setValue(Task.DETAILS, null);
            item.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }
        return super.saveExisting(item);
    }

    /**
     * Called after the task is saved.
     * <ul>
     * <li>Handle repeating tasks
     * <li>Save for synchronization
     *
     * @param database
     * @param task task that was just changed
     * @param values values to be persisted to the database
     * @param skipHooks whether this save occurs as part of a sync
     */
    private void afterSave(Task task, ContentValues values) {
        if(values != null) {
            if(values.containsKey(Task.COMPLETION_DATE.name) && task.isCompleted())
                afterComplete(task, values);
            else if(values.containsKey(Task.DUE_DATE.name) ||
                    values.containsKey(Task.REMINDER_FLAGS.name) ||
                    values.containsKey(Task.REMINDER_PERIOD.name) ||
                    values.containsKey(Task.REMINDER_LAST.name) ||
                    values.containsKey(Task.REMINDER_SNOOZE.name))
                ReminderService.getInstance().scheduleAlarm(task);
        }

        afterTasklistChange();
    }

    /**
     * Called when task list has changed
     */
    private void afterTasklistChange() {
        Astrid2TaskProvider.notifyDatabaseModification();
        TasksWidget.updateWidgets(ContextManager.getContext());
        PowerWidget.updateWidgets(ContextManager.getContext());
        PowerWidget42.updateWidgets(ContextManager.getContext());
    }

    /**
     * Called after the task was just completed
     *
     * @param task
     * @param values
     * @param duringSync
     */
    private void afterComplete(Task task, ContentValues values) {
        // send broadcast
        Context context = ContextManager.getContext();
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
        context.sendOrderedBroadcast(broadcastIntent, null);

        Notifications.cancelNotifications(task.getId());
    }

}

