/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.data.task;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.task.AbstractTaskModel.TaskModelDatabaseHelper;
import com.timsu.astrid.utilities.Notifications;

/** Controller for task-related operations */
public class TaskController extends AbstractController {

    private SQLiteDatabase database;

    // --- task list operations

    /** Return a list of all active tasks with notifications */
    public List<TaskModelForNotify> getTasksWithNotifications() {
        List<TaskModelForNotify> list = new ArrayList<TaskModelForNotify>();
        Cursor cursor = database.query(TASK_TABLE_NAME, TaskModelForNotify.FIELD_LIST,
                String.format("%s < %d AND %s != 0",
                        AbstractTaskModel.PROGRESS_PERCENTAGE,
                        AbstractTaskModel.COMPLETE_PERCENTAGE,
                        AbstractTaskModel.NOTIFICATIONS), null, null, null, null, null);

        if(cursor.getCount() == 0)
            return list;

        do {
            cursor.moveToNext();
            list.add(new TaskModelForNotify(cursor));
        } while(!cursor.isLast());

        cursor.close();
        return list;
    }

    /** Return a list of all active tasks with deadlines */
    public List<TaskModelForNotify> getTasksWithDeadlines() {
        List<TaskModelForNotify> list = new ArrayList<TaskModelForNotify>();
        Cursor cursor = database.query(TASK_TABLE_NAME, TaskModelForNotify.FIELD_LIST,
                String.format("%s < %d AND (%s != 0 OR %s != 0)",
                        AbstractTaskModel.PROGRESS_PERCENTAGE,
                        AbstractTaskModel.COMPLETE_PERCENTAGE,
                        AbstractTaskModel.DEFINITE_DUE_DATE,
                        AbstractTaskModel.PREFERRED_DUE_DATE), null, null, null, null, null);

        if(cursor.getCount() == 0)
            return list;

        do {
            cursor.moveToNext();
            list.add(new TaskModelForNotify(cursor));
        } while(!cursor.isLast());

        cursor.close();
        return list;
    }

    /** Return a list of all of the tasks with progress < COMPLETE_PERCENTAGE */
    public Cursor getActiveTaskListCursor() {
        return database.query(TASK_TABLE_NAME, TaskModelForList.FIELD_LIST,
            AbstractTaskModel.PROGRESS_PERCENTAGE + " < " +
                AbstractTaskModel.COMPLETE_PERCENTAGE, null, null, null,
                null, null);
    }

    /** Return a list of all tasks */
    public Cursor getAllTaskListCursor() {
        return database.query(TASK_TABLE_NAME, TaskModelForList.FIELD_LIST,
                null, null, null, null, null, null);
    }

    /** Create a weighted list of tasks from the db cursor given */
    public List<TaskModelForList> createTaskListFromCursor(Cursor cursor,
            boolean hideHidden) {
        List<TaskModelForList> list = new ArrayList<TaskModelForList>();

        if(cursor.getCount() == 0)
            return list;

        do {
            cursor.moveToNext();
            list.add(new TaskModelForList(cursor));
        } while(!cursor.isLast());

        return TaskModelForList.sortAndFilterList(list, hideHidden);
    }

    /** Create a weighted list of tasks from the db cursor given */
    public Cursor getTaskListCursorById(List<TaskIdentifier> idList) {

        StringBuilder where = new StringBuilder();
        for(int i = 0; i < idList.size(); i++) {
            where.append(KEY_ROWID);
            where.append("=");
            where.append(idList.get(i).idAsString());
            if(i < idList.size()-1)
                where.append(" OR ");
        }

        // hack for empty arrays
        if(idList.size() == 0)
            where.append("0");

        return database.query(true, TASK_TABLE_NAME,
                TaskModelForList.FIELD_LIST, where.toString(), null, null,
                null, null, null);
    }

    // --- single task operations

    /** Delete the given task */
    public boolean deleteTask(TaskIdentifier taskId) {
        if(taskId == null)
            throw new UnsupportedOperationException("Cannot delete uncreated task!");
        long id = taskId.getId();
        Notifications.deleteAlarm(context, id);
        return database.delete(TASK_TABLE_NAME, KEY_ROWID + "=" + id, null) > 0;
    }

    /** Saves the given task to the database. Returns true on success. */
    public boolean saveTask(AbstractTaskModel task) {
        boolean saveSucessful;

        if(task.getTaskIdentifier() == null) {
            long newRow = database.insert(TASK_TABLE_NAME, AbstractTaskModel.NAME,
                    task.getMergedValues());
            task.setTaskIdentifier(new TaskIdentifier(newRow));

            saveSucessful = newRow >= 0;
        } else {
            long id = task.getTaskIdentifier().getId();
            ContentValues values = task.getSetValues();

            // set completion date
            if(values.containsKey(AbstractTaskModel.PROGRESS_PERCENTAGE) &&
                    values.getAsInteger(AbstractTaskModel.PROGRESS_PERCENTAGE)
                        == AbstractTaskModel.COMPLETE_PERCENTAGE) {
                values.put(AbstractTaskModel.COMPLETION_DATE, System.currentTimeMillis());
            }

            saveSucessful = database.update(TASK_TABLE_NAME, task.getSetValues(),
                    KEY_ROWID + "=" + id, null) > 0;
        }

        return saveSucessful;
    }

    // --- fetching different models

    /**  Creates a new task and returns the task identifier */
    public TaskModelForEdit createNewTaskForEdit() {
        TaskModelForEdit task = new TaskModelForEdit();
        task.setTaskIdentifier(null);

        return task;
    }

    /** Returns a TaskModelForEdit corresponding to the given TaskIdentifier */
    public TaskModelForEdit fetchTaskForEdit(Activity activity, TaskIdentifier
            taskId) throws SQLException {
        long id = taskId.getId();
        Cursor cursor = database.query(true, TASK_TABLE_NAME,
                TaskModelForEdit.FIELD_LIST,
                KEY_ROWID + "=" + id, null, null, null, null, null);
        activity.startManagingCursor(cursor);
        if (cursor != null) {
            cursor.moveToFirst();
            TaskModelForEdit model = new TaskModelForEdit(taskId, cursor);
            return model;
        }

        throw new SQLException("Returned empty set!");

    }


    /** Returns a TaskModelForView corresponding to the given TaskIdentifier */
    public TaskModelForView fetchTaskForView(Activity activity,
            TaskIdentifier taskId) throws SQLException {
        long id = taskId.getId();
        Cursor cursor = database.query(true, TASK_TABLE_NAME,
                TaskModelForView.FIELD_LIST,
                KEY_ROWID + "=" + id, null, null, null, null, null);
        activity.startManagingCursor(cursor);
        if (cursor != null) {
            cursor.moveToFirst();
            TaskModelForView model = new TaskModelForView(taskId, cursor);
            return model;
        }

        throw new SQLException("Returned empty set!");
    }

    /** Returns a TaskModelForView corresponding to the given TaskIdentifier */
    public TaskModelForList fetchTaskForList(TaskIdentifier taskId) throws SQLException {
        long id = taskId.getId();
        Cursor cursor = database.query(true, TASK_TABLE_NAME,
                TaskModelForList.FIELD_LIST,
                KEY_ROWID + "=" + id, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            TaskModelForList model = new TaskModelForList(cursor);
            cursor.close();

            return model;
        }

        throw new SQLException("Returned empty set!");
    }

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public TaskController(Context activity) {
        this.context = activity;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public TaskController open() throws SQLException {
        SQLiteOpenHelper databaseHelper = new TaskModelDatabaseHelper(
                context, TASK_TABLE_NAME, TASK_TABLE_NAME);
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    /** Closes database resource */
    public void close() {
        database.close();
    }
}
