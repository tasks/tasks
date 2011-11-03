/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Flags;

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

    	/** Check if a given task belongs to someone else & is read-only */
        public static Criterion ownedByMe() {
             return Criterion.and(Field.field(Task.FLAGS.name+ " & " + //$NON-NLS-1$
                     Task.FLAG_IS_READONLY).eq(0),
                     Task.USER_ID.eq(0));
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
    	public static Criterion activeVisibleMine() {
            return Criterion.and(Task.COMPLETION_DATE.eq(0),
                    Task.DELETION_DATE.eq(0),
                    Task.HIDE_UNTIL.lt(Functions.now()),
                    Field.field(Task.FLAGS.name + " & " + //$NON-NLS-1$
                            Task.FLAG_IS_READONLY).eq(0),
                    Task.USER_ID.eq(0));
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

        TaskApiDao.afterTaskListChanged();

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
        if (task.getId() == Task.NO_ID) {
            saveSuccessful = createNew(task);
        } else {
            saveSuccessful = saveExisting(task);
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
            item.setValue(Task.DUE_DATE, Task.createDueDate(setting, 0));
        }
        if(!item.containsValue(Task.HIDE_UNTIL)) {
            int setting = Preferences.getIntegerFromString(R.string.p_default_hideUntil_key,
                    Task.HIDE_UNTIL_NONE);
            item.setValue(Task.HIDE_UNTIL, item.createHideUntil(setting, 0));
        }

        setDefaultReminders(item);

        ContentValues values = item.getSetValues();
        boolean result = super.createNew(item);
        if(result) {
            if(Preferences.getBoolean(AstridPreferences.P_FIRST_ACTION, false)) {
                StatisticsService.reportEvent(StatisticsConstants.USER_FIRST_TASK);
                Preferences.setBoolean(AstridPreferences.P_FIRST_ACTION, false);
            }
            afterSave(item, values);
        }
        return result;
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
        if(!TaskApiDao.insignificantChange(values))    {
            item.setValue(Task.DETAILS, null);
            if(!values.containsKey(Task.MODIFICATION_DATE.name))
                item.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }
        boolean result = super.saveExisting(item);
        if(result)
            afterSave(item, values);
        return result;
    }

    /**
     * Called after the task is saved. This differs from the call in
     * TaskApiDao in that it runs hooks that need to be run from within
     * Astrid. Order matters here!
     */
    public static void afterSave(Task task, ContentValues values) {
        if(values == null || Flags.checkAndClear(Flags.SUPPRESS_HOOKS))
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
        TaskApiDao.afterSave(task, values);
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

