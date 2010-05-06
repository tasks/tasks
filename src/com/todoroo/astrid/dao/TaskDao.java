/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.todoroo.andlib.data.AbstractDao;
import com.todoroo.andlib.data.AbstractDatabase;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.MetadataDao.MetadataSql;
import com.todoroo.astrid.model.Task;

/**
 * Data Access layer for {@link Task}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskDao extends AbstractDao<Task> {

    @Autowired
    MetadataDao metadataDao;

	public TaskDao() {
        super(Task.class);
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class TaskSql {

    	/** Returns tasks by id */
    	public static String byId(long id) {
    	    return String.format("(%s = %d)", Task.ID, //$NON-NLS-1$
    	            id);
    	}

    	/** Return tasks that have not yet been completed */
    	public static String isActive() {
            return String.format("(%s = 0 AND %s = 0)", Task.COMPLETION_DATE, //$NON-NLS-1$
                    Task.DELETION_DATE);
    	}

    	/** Return tasks that are not hidden at given unixtime */
    	public static String isVisible(int time) {
            return String.format("(%s < %d)", Task.HIDDEN_UNTIL, time); //$NON-NLS-1$
        }

    	/** Returns tasks that have a due date */
    	public static String hasDeadlines() {
    		return String.format("(%s != 0)", Task.DUE_DATE); //$NON-NLS-1$
    	}

        /** Returns tasks that are due before a certain unixtime */
        public static String dueBefore(int time) {
            return String.format("(%s > 0 AND %s <= %d)", Task.DUE_DATE, //$NON-NLS-1$
                    Task.DUE_DATE, time);
        }

        /** Returns tasks that are due after a certain unixtime */
        public static String dueAfter(int time) {
            return String.format("(%s > %d)", Task.DUE_DATE, time); //$NON-NLS-1$
        }

    	/** Returns tasks completed before a given unixtime */
    	public static String completedBefore(int time) {
    		return String.format("(%s > 0 AND %s < %d)", Task.COMPLETION_DATE, //$NON-NLS-1$
    		        Task.COMPLETION_DATE, time);
    	}

        public static String hasNoName() {
            return String.format("(%s = \"\")", Task.TITLE); //$NON-NLS-1$
        }

    }

    // --- custom operations

    /**
     * Return cursor to all tasks matched by given filter
     *
     * @param database
     * @param properties
     *            properties to read from task
     * @param filter
     *            {@link Filter} object to use
     * @return
     */
    public TodorooCursor<Task> fetch(Database database, Property<?>[] properties,
            Filter filter) {

        String query = String.format(
                "SELECT %s FROM %s %s ", //$NON-NLS-1$
                propertiesForSelect(properties, true),
                Database.TASK_TABLE,
                filter.sqlQuery);
        Cursor cursor = database.getDatabase().rawQuery(query, null);
        return new TodorooCursor<Task>(cursor);
    }

    // --- delete

    /**
     * Delete the given item
     *
     * @param database
     * @param id
     * @return true if delete was successful
     */
    @Override
    public boolean delete(AbstractDatabase database, long id) {
        boolean result = super.delete(database, id);
        if(!result)
            return false;

        // delete all metadata
        metadataDao.deleteWhere(database, MetadataSql.byTask(id));

        return true;
    }

    // --- save

    /**
     * Saves the given task to the database.getDatabase(). Task must already
     * exist. Returns true on success.
     *
     * @param duringSync whether this save occurs as part of a sync
     */
    public boolean save(Database database, Task task, boolean duringSync) {
        boolean saveSuccessful;

        if (task.getId() == Task.NO_ID) {
            task.setValue(Task.CREATION_DATE, DateUtilities.now());
            saveSuccessful = createItem(database, Database.TASK_TABLE, task);

            Context context = ContextManager.getContext();
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_CREATED);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            context.sendOrderedBroadcast(broadcastIntent, null);
        } else {
            ContentValues values = task.getSetValues();

            if(values.size() == 0) // nothing changed
                return true;

            beforeSave(database, task, values, duringSync);
            saveSuccessful = saveItem(database, Database.TASK_TABLE, task);
            afterSave(database, task, values, duringSync);

        }

        return saveSuccessful;
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
    private void beforeSave(Database database, Task task, ContentValues values, boolean duringSync) {
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
    private void afterSave(Database database, Task task, ContentValues values, boolean duringSync) {
        if(duringSync)
            return;

        // if task was completed, fire task completed notification
        if(values.containsKey(Task.COMPLETION_DATE.name) &&
                values.getAsInteger(Task.COMPLETION_DATE.name) > 0 && !duringSync) {

            Context context = ContextManager.getContext();
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            context.sendOrderedBroadcast(broadcastIntent, null);

        }
    }

}

