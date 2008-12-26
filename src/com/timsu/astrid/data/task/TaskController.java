package com.timsu.astrid.data.task;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.task.AbstractTaskModel.TaskModelDatabaseHelper;

public class TaskController extends AbstractController {

    private SQLiteDatabase database;

    // --- task list operations

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
    public TaskModelForEdit fetchTaskForEdit(TaskIdentifier
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
    public TaskModelForView fetchTaskForView(TaskIdentifier taskId) throws SQLException {
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

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public TaskController(Activity activity) {
        this.activity = activity;
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
                activity, TASK_TABLE_NAME, TASK_TABLE_NAME);
        database = databaseHelper.getWritableDatabase();
        return this;
    }

    /** Closes database resource */
    public void close() {
        database.close();
    }
}
