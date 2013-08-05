/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.timsu.astrid.data.task.AbstractTaskModel.TaskModelDatabaseHelper;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.widget.TasksWidget.WidgetUpdateService;

import java.util.Date;

/**
 * Controller for task-related operations
 *
 * @author timsu
 */
@Deprecated

public class TaskController extends LegacyAbstractController {

    private SQLiteDatabase database;

    // --- task list operations

    /**
     * Return a list of all tasks
     */
    public Cursor getBackupTaskListCursor() {
        return database.query(tasksTable, TaskModelForXml.FIELD_LIST,
                null, null, null, null,
                null, null);
    }

    // --- single task operations

    /**
     * Delete the given task
     */
    public boolean deleteTask(TaskIdentifier taskId) {
        if (taskId == null) {
            throw new UnsupportedOperationException("Cannot delete uncreated task!");
        }
        long id = taskId.getId();
        cleanupTask(taskId, false);

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

        return database.delete(tasksTable, KEY_ROWID + "=" + id, null) > 0;
    }

    /**
     * Saves the given task to the database. Returns true on success.
     *
     * @param duringSync set to true when save is part of a synchronize
     */
    public boolean saveTask(AbstractTaskModel task, boolean duringSync) {
        boolean saveSucessful;

        if (task.getTaskIdentifier() == null) {
            long newRow = database.insert(tasksTable, AbstractTaskModel.NAME,
                    task.getMergedValues());
            task.setTaskIdentifier(new TaskIdentifier(newRow));

            saveSucessful = newRow >= 0;
        } else {
            long id = task.getTaskIdentifier().getId();
            ContentValues values = task.getSetValues();

            if (values.size() == 0) // nothing changed
            {
                return true;
            }

            onTaskSave(task, values);

            saveSucessful = database.update(tasksTable, values,
                    KEY_ROWID + "=" + id, null) > 0;

            // task was completed
            if (values.containsKey(AbstractTaskModel.PROGRESS_PERCENTAGE) &&
                    values.getAsInteger(AbstractTaskModel.PROGRESS_PERCENTAGE)
                            == AbstractTaskModel.COMPLETE_PERCENTAGE) {
                onTaskCompleted(task, values, duringSync);
            }

            SyncDataController.taskUpdated(context, task);
        }

        // notify widget that something changed
        if (saveSucessful) {
            Intent intent = new Intent(context, WidgetUpdateService.class);
            context.startService(intent);
        }

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

        return saveSucessful;
    }

    /**
     * Called when the task is saved. Perform some processing on the task.
     *
     * @param task
     * @param values
     * @param duringSync
     */
    private void onTaskSave(AbstractTaskModel task, ContentValues values) {
        // save task completed date
        if (values.containsKey(AbstractTaskModel.PROGRESS_PERCENTAGE) &&
                values.getAsInteger(AbstractTaskModel.PROGRESS_PERCENTAGE)
                        == AbstractTaskModel.COMPLETE_PERCENTAGE) {
            values.put(AbstractTaskModel.COMPLETION_DATE, System.currentTimeMillis());
        }

        // task timer was updated, update notification bar
        if (values.containsKey(AbstractTaskModel.TIMER_START)) {
        }

        // due date was updated, update calendar event
        if ((values.containsKey(AbstractTaskModel.DEFINITE_DUE_DATE) ||
                values.containsKey(AbstractTaskModel.PREFERRED_DUE_DATE)) &&
                !values.containsKey(AbstractTaskModel.CALENDAR_URI)) {
            try {
                Cursor cursor = fetchTaskCursor(task.getTaskIdentifier(),
                        new String[]{AbstractTaskModel.CALENDAR_URI});
                cursor.moveToFirst();
                String uriAsString = cursor.getString(0);
                cursor.close();
                if (uriAsString != null && uriAsString.length() > 0) {
                    ContentResolver cr = context.getContentResolver();
                    Uri uri = Uri.parse(uriAsString);

                    // create new start and end date for this event
                    ContentValues newValues = new ContentValues();
                    cr.update(uri, newValues, null, null);
                }
            } catch (Exception e) {
                // ignore calendar event - event could be deleted or whatever
                Log.e("astrid", "Error moving calendar event", e);
            }
        }
    }


    /**
     * Called when this task is set to completed.
     *
     * @param task   task to process
     * @param values mutable map of values to save
     */
    private void onTaskCompleted(AbstractTaskModel task, ContentValues values, boolean duringSync) {
        Cursor cursor = fetchTaskCursor(task.getTaskIdentifier(),
                TaskModelForHandlers.FIELD_LIST);
        TaskModelForHandlers model = new TaskModelForHandlers(cursor, values);

        // handle repeat
        RepeatInfo repeatInfo = model.getRepeat();
        if (repeatInfo != null) {
            model.repeatTaskBy(context, repeatInfo);
            database.update(tasksTable, values, KEY_ROWID + "=" +
                    task.getTaskIdentifier().getId(), null);
        }

        // handle sync-on-complete
        if ((model.getFlags() & TaskModelForHandlers.FLAG_SYNC_ON_COMPLETE) > 0 &&
                !duringSync) {
        }

        cursor.close();
        cleanupTask(task.getTaskIdentifier(), repeatInfo != null);
    }

    /**
     * Clean up state from a task. Called when deleting or completing it
     */
    private void cleanupTask(TaskIdentifier taskId, boolean isRepeating) {
        // delete calendar event if not repeating
        if (!isRepeating) {
            try {
                Cursor cursor = fetchTaskCursor(taskId, new String[]{
                        AbstractTaskModel.CALENDAR_URI});
                cursor.moveToFirst();
                String uri = cursor.getString(0);
                cursor.close();
                if (uri != null && uri.length() > 0) {
                    ContentResolver cr = context.getContentResolver();
                    cr.delete(Uri.parse(uri), null, null);
                    ContentValues values = new ContentValues();
                    values.put(AbstractTaskModel.CALENDAR_URI, (String) null);
                    database.update(tasksTable, values, KEY_ROWID + "=" +
                            taskId.getId(), null);
                }
            } catch (Exception e) {
                Log.e("astrid", "Error deleting calendar event", e);
            }
        }
    }

    /**
     * Set last notification date
     */
    public boolean setLastNotificationTime(TaskIdentifier taskId, Date date) {
        ContentValues values = new ContentValues();
        values.put(AbstractTaskModel.LAST_NOTIFIED, date.getTime());
        return database.update(tasksTable, values,
                KEY_ROWID + "=" + taskId.getId(), null) > 0;
    }

    // --- fetching different models

    /**
     * Moves cursor to the task.
     * Don't forget to close the cursor when you're done.
     */
    private Cursor fetchTaskCursor(TaskIdentifier taskId, String[] fieldList) {
        long id = taskId.getId();
        Cursor cursor = database.query(true, tasksTable, fieldList,
                KEY_ROWID + "=" + id, null, null, null, null, null);
        if (cursor == null) {
            throw new SQLException("Returned empty set!");
        }

        cursor.moveToFirst();
        return cursor;
    }

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public TaskController(Context context) {
        super(context);
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     * initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    @Override
    public synchronized void open() throws SQLException {
        SQLiteOpenHelper databaseHelper = new TaskModelDatabaseHelper(
                context, tasksTable, tasksTable);
        database = databaseHelper.getWritableDatabase();
    }

    /**
     * Closes database resource
     */
    @Override
    public void close() {
        database.close();
    }
}
