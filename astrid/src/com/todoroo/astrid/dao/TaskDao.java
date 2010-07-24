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
import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.utility.Preferences;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskDao extends GenericDao<Task> {

    @Autowired
    MetadataDao metadataDao;

    @Autowired
    Database database;

    @Autowired
    ExceptionService exceptionService;

    ReminderService reminderService;

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

        return true;
    }

    // --- save

    /**
     * Saves the given task to the database.getDatabase(). Task must already
     * exist. Returns true on success.
     *
     * @param task
     * @param skipHooks
     *            Whether pre and post hooks should run. This should be set
     *            to true if tasks are created as part of synchronization
     * @return true if save occurred, false otherwise (i.e. nothing changed)
     */
    public boolean save(Task task, boolean skipHooks) {
        boolean saveSuccessful;

        ContentValues values = task.getSetValues();
        if(values == null || values.size() == 0)
            return false;

        if (task.getId() == Task.NO_ID) {
            saveSuccessful = createNew(task);
        } else {
            beforeSave(task, values, skipHooks);
            saveSuccessful = saveExisting(task);
            afterSave(task, values, skipHooks);
        }

        if(saveSuccessful)
            task.markSaved();

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
                    R.string.p_default_importance_key));
        if(!item.containsValue(Task.DUE_DATE)) {
            int setting = Preferences.getIntegerFromString(R.string.p_default_urgency_key);
            item.setValue(Task.DUE_DATE, item.createDueDate(setting, 0));
        }
        if(!item.containsValue(Task.HIDE_UNTIL)) {
            int setting = Preferences.getIntegerFromString(R.string.p_default_hideUntil_key);
            item.setValue(Task.HIDE_UNTIL, item.createHideUntil(setting, 0));
        }
        if(!item.containsValue(Task.REMINDER_PERIOD)) {
            item.setValue(Task.REMINDER_PERIOD, DateUtilities.ONE_HOUR *
                    Preferences.getIntegerFromString(R.string.p_rmd_default_random_hours));
        }
        if(!item.containsValue(Task.REMINDER_FLAGS)) {
            item.setValue(Task.REMINDER_FLAGS, Task.NOTIFY_AT_DEADLINE);
        }

        return super.createNew(item);
    }

    @Override
    public boolean saveExisting(Task item) {
        item.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        return super.saveExisting(item);
    }

    /**
     * Called before the task is saved.
     * <ul>
     * <li>Update notifications based on task status
     * <li>Update associated calendar event
     *
     * @param database
     * @param task
     *            task that was just changed
     * @param values
     *            values that were changed
     * @param duringSync
     *            whether this save occurs as part of a sync
     */
    private void beforeSave(Task task, ContentValues values, boolean duringSync) {
        //
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
     * @param duringSync whether this save occurs as part of a sync
     */
    private void afterSave(Task task, ContentValues values, boolean duringSync) {
        if(values.containsKey(Task.COMPLETION_DATE.name) && task.isCompleted())
            afterComplete(task, values, duringSync);
        else {
            if(reminderService == null)
                reminderService = new ReminderService();
            reminderService.scheduleAlarm(task);
        }

        if(duringSync)
            return;

        // due date was updated, update calendar event
        /*if((values.containsKey(AbstractTaskModel.DEFINITE_DUE_DATE) ||
                values.containsKey(AbstractTaskModel.PREFERRED_DUE_DATE)) &&
                !values.containsKey(AbstractTaskModel.CALENDAR_URI)) {
            try {
                Cursor cursor = fetchTaskCursor(task.getTaskIdentifier(),
                        new String[] { AbstractTaskModel.CALENDAR_URI });
                cursor.moveToFirst();
                String uriAsString = cursor.getString(0);
                cursor.close();
                if(uriAsString != null && uriAsString.length() > 0) {
                    ContentResolver cr = context.getContentResolver();
                    Uri uri = Uri.parse(uriAsString);

                    Integer estimated = null;
                    if(values.containsKey(AbstractTaskModel.ESTIMATED_SECONDS))
                        estimated = values.getAsInteger(AbstractTaskModel.ESTIMATED_SECONDS);
                    else { // read from event
                        Cursor event = cr.query(uri, new String[] {"dtstart", "dtend"},
                                null, null, null);
                        event.moveToFirst();
                        estimated = (event.getInt(1) - event.getInt(0))/1000;
                    }

                    // create new start and end date for this event
                    ContentValues newValues = new ContentValues();
                    TaskEditActivity.createCalendarStartEndTimes(task.getPreferredDueDate(),
                            task.getDefiniteDueDate(), estimated, newValues); TODO
                    cr.update(uri, newValues, null, null);
                }
            } catch (Exception e) {
                // ignore calendar event - event could be deleted or whatever
                Log.e("astrid", "Error moving calendar event", e);
            }
        }*/
    }

    /**
     * Called after the task was just completed
     *
     * @param task
     * @param values
     * @param duringSync
     */
    private void afterComplete(Task task, ContentValues values, boolean duringSync) {


        /*Cursor cursor = fetchTaskCursor(task.getTaskIdentifier(),
                TaskModelForHandlers.FIELD_LIST);
        TaskModelForHandlers model = new TaskModelForHandlers(cursor, values);

        // handle repeat
        RepeatInfo repeatInfo = model.getRepeat();
        if(repeatInfo != null) {
            model.repeatTaskBy(context, this, repeatInfo);
            database.update(tasksTable, values, KEY_ROWID + "=" +
                    task.getTaskIdentifier().getId(), null);
        }

        // handle sync-on-complete
        if((model.getFlags() & TaskModelForHandlers.FLAG_SYNC_ON_COMPLETE) > 0 &&
                !duringSync) {
            Synchronizer synchronizer = new Synchronizer(model.getTaskIdentifier());
            synchronizer.synchronize(context, new SynchronizerListener() {
                public void onSynchronizerFinished(int numServicesSynced) {
                    TaskListSubActivity.shouldRefreshTaskList = true;
                }
            });
        }

        cursor.close();
        cleanupTask(task.getTaskIdentifier(), repeatInfo != null);*/

        // send broadcast
        if(!duringSync) {
            Context context = ContextManager.getContext();
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            context.sendOrderedBroadcast(broadcastIntent, null);
        }
    }

}

