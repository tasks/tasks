/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
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
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;
import com.timsu.astrid.data.task.AbstractTaskModel.TaskModelDatabaseHelper;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.widget.TasksWidget.WidgetUpdateService;

/**
 * Controller for task-related operations
 *
 * @author timsu
 *
 */
@Deprecated
@SuppressWarnings("nls")
public class TaskController extends LegacyAbstractController {

    private SQLiteDatabase database;

    // --- task list operations

    /** Return a list of all active tasks with notifications */
    public HashSet<TaskModelForNotify> getTasksWithNotifications() {
        HashSet<TaskModelForNotify> list = new HashSet<TaskModelForNotify>();
        Cursor cursor = database.query(tasksTable, TaskModelForNotify.FIELD_LIST,
                String.format("%s < %d AND (%s != 0 OR %s != 0)",
                        AbstractTaskModel.PROGRESS_PERCENTAGE,
                        AbstractTaskModel.COMPLETE_PERCENTAGE,
                        AbstractTaskModel.NOTIFICATIONS,
                        AbstractTaskModel.NOTIFICATION_FLAGS), null, null, null, null, null);

        try {
            if(cursor.getCount() == 0)
                return list;
            do {
                cursor.moveToNext();
                list.add(new TaskModelForNotify(cursor));
            } while(!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /** Return a list of all active tasks with deadlines */
    public ArrayList<TaskModelForNotify> getTasksWithDeadlines() {
        ArrayList<TaskModelForNotify> list = new ArrayList<TaskModelForNotify>();
        Cursor cursor = database.query(tasksTable, TaskModelForNotify.FIELD_LIST,
                String.format("%s < %d AND (%s != 0 OR %s != 0)",
                        AbstractTaskModel.PROGRESS_PERCENTAGE,
                        AbstractTaskModel.COMPLETE_PERCENTAGE,
                        AbstractTaskModel.DEFINITE_DUE_DATE,
                        AbstractTaskModel.PREFERRED_DUE_DATE), null, null, null, null, null);

        try {
            if(cursor.getCount() == 0)
                return list;

            do {
                cursor.moveToNext();
                list.add(new TaskModelForNotify(cursor));
            } while(!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /** Return a list of all of the tasks with progress < COMPLETE_PERCENTAGE */
    public Cursor getActiveTaskListCursor() {
        return database.query(tasksTable, TaskModelForList.FIELD_LIST,
            AbstractTaskModel.PROGRESS_PERCENTAGE + " < " +
                AbstractTaskModel.COMPLETE_PERCENTAGE, null, null, null,
                null, null);
    }

    /** Return a list of all of the tasks matching selection */
    public Cursor getMatchingTasksForProvider(String selection,
                String[] selectionArgs) {
        return database.query(tasksTable, TaskModelForProvider.FIELD_LIST,
                selection, selectionArgs, null, null,
                null, null);
    }

    /** Return a list of all tasks */
    public Cursor getAllTaskListCursor() {
        return database.query(tasksTable, TaskModelForList.FIELD_LIST,
                null, null, null, null, null, null);
    }

    /** Return a list of all tasks */
    public Cursor getBackupTaskListCursor() {
        return database.query(tasksTable, TaskModelForXml.FIELD_LIST,
                null, null, null, null,
                null, null);
    }

    /** Delete all completed tasks with date < older than date */
    public int deleteCompletedTasksOlderThan(Date olderThanDate) {
        return database.delete(tasksTable, String.format("`%s` >= '%d' AND `%s` <= '%d'",
                AbstractTaskModel.PROGRESS_PERCENTAGE, AbstractTaskModel.COMPLETE_PERCENTAGE,
                AbstractTaskModel.COMPLETION_DATE, olderThanDate.getTime()), null);
    }

    /** Create a list of tasks from the db cursor given */
    public ArrayList<TaskModelForList> createTaskListFromCursor(Cursor cursor) {
        ArrayList<TaskModelForList> list = new ArrayList<TaskModelForList>();

        if(cursor.getCount() == 0)
            return list;

        do {
            cursor.moveToNext();
            list.add(new TaskModelForList(cursor));
        } while(!cursor.isLast());

        return list;
    }

    /** Helper method to take a cursor pointing to a list of id's and generate
     * a hashset */
    private HashSet<TaskIdentifier> createTaskIdentifierSet(Cursor cursor) {
        HashSet<TaskIdentifier> list = new HashSet<TaskIdentifier>();
        try {
            if(cursor.getCount() == 0)
                return list;

            do {
                cursor.moveToNext();
                list.add(new TaskIdentifier(cursor.getInt(
                        cursor.getColumnIndexOrThrow(KEY_ROWID))));
            } while(!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /** Get identifiers for all tasks */
    public HashSet<TaskIdentifier> getAllTaskIdentifiers() {
        Cursor cursor = database.query(tasksTable, new String[] { KEY_ROWID },
                null, null, null, null, null, null);
        return createTaskIdentifierSet(cursor);
    }

    /** Get identifiers for all non-completed tasks */
    public HashSet<TaskIdentifier> getActiveTaskIdentifiers() {
        Cursor cursor = database.query(tasksTable, new String[] { KEY_ROWID },
                AbstractTaskModel.PROGRESS_PERCENTAGE + " < " +
                AbstractTaskModel.COMPLETE_PERCENTAGE, null, null, null, null, null);
        return createTaskIdentifierSet(cursor);
    }

    /** Get identifiers for all non-completed, non-hidden tasks */
    public HashSet<TaskIdentifier> getActiveVisibleTaskIdentifiers() {
        Cursor cursor = database.query(tasksTable, new String[] { KEY_ROWID },
                AbstractTaskModel.PROGRESS_PERCENTAGE + " < " +
                AbstractTaskModel.COMPLETE_PERCENTAGE + " AND (" +
                AbstractTaskModel.HIDDEN_UNTIL + " ISNULL OR " + AbstractTaskModel.HIDDEN_UNTIL + " < " +
                System.currentTimeMillis() + ")", null, null, null, null, null);
        return createTaskIdentifierSet(cursor);
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

        return database.query(true, tasksTable,
                TaskModelForList.FIELD_LIST, where.toString(), null, null,
                null, null, null);
    }

    // --- single task operations

    /** Delete the given task */
    public boolean deleteTask(TaskIdentifier taskId) {
        if(taskId == null)
            throw new UnsupportedOperationException("Cannot delete uncreated task!");
        long id = taskId.getId();
        cleanupTask(taskId, false);

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

        return database.delete(tasksTable, KEY_ROWID + "=" + id, null) > 0;
    }

    /** Saves the given task to the database. Returns true on success.
     *
     * @param duringSync set to true when save is part of a synchronize
     */
    public boolean saveTask(AbstractTaskModel task, boolean duringSync) {
        boolean saveSucessful;

        if(task.getTaskIdentifier() == null) {
            long newRow = database.insert(tasksTable, AbstractTaskModel.NAME,
                    task.getMergedValues());
            task.setTaskIdentifier(new TaskIdentifier(newRow));

            saveSucessful = newRow >= 0;
        } else {
            long id = task.getTaskIdentifier().getId();
            ContentValues values = task.getSetValues();

            if(values.size() == 0) // nothing changed
                return true;

            onTaskSave(task, values, duringSync);

            saveSucessful = database.update(tasksTable, values,
                    KEY_ROWID + "=" + id, null) > 0;

            // task was completed
            if(values.containsKey(AbstractTaskModel.PROGRESS_PERCENTAGE) &&
                    values.getAsInteger(AbstractTaskModel.PROGRESS_PERCENTAGE)
                        == AbstractTaskModel.COMPLETE_PERCENTAGE) {
                onTaskCompleted(task, values, duringSync);
            }

            SyncDataController.taskUpdated(context, task);
        }

        // notify widget that something changed
        if(saveSucessful) {
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
    private void onTaskSave(AbstractTaskModel task, ContentValues values, boolean duringSync) {
        // save task completed date
        if(values.containsKey(AbstractTaskModel.PROGRESS_PERCENTAGE) &&
                values.getAsInteger(AbstractTaskModel.PROGRESS_PERCENTAGE)
                    == AbstractTaskModel.COMPLETE_PERCENTAGE) {
            values.put(AbstractTaskModel.COMPLETION_DATE, System.currentTimeMillis());
        }

        // task timer was updated, update notification bar
        if(values.containsKey(AbstractTaskModel.TIMER_START)) {
        	// show notification bar if timer was started
//        	if(values.getAsLong(AbstractTaskModel.TIMER_START) != 0) {
//        		ReminderService.showTimingNotification(context,
//        				task.getTaskIdentifier(), task.getName());
//        	} else {
//        		ReminderService.clearAllNotifications(context, task.getTaskIdentifier());
//        	}
        }

        // due date was updated, update calendar event
        if((values.containsKey(AbstractTaskModel.DEFINITE_DUE_DATE) ||
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

//                    Integer estimated = null;
//                    if(values.containsKey(AbstractTaskModel.ESTIMATED_SECONDS))
////                        estimated = values.getAsInteger(AbstractTaskModel.ESTIMATED_SECONDS);
//                    else { // read from event
//                        Cursor event = cr.query(uri, new String[] {"dtstart", "dtend"},
//                                null, null, null);
//                        event.moveToFirst();
////                        estimated = (event.getInt(1) - event.getInt(0))/1000;
//                    }

                    // create new start and end date for this event
                    ContentValues newValues = new ContentValues();
                    /*TaskEditActivity.createCalendarStartEndTimes(task.getPreferredDueDate(),
                            task.getDefiniteDueDate(), estimated, newValues); TODO */
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
     * @param task task to process
     * @param values mutable map of values to save
     */
    private void onTaskCompleted(AbstractTaskModel task, ContentValues values, boolean duringSync) {
        Cursor cursor = fetchTaskCursor(task.getTaskIdentifier(),
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
//            Synchronizer synchronizer = new Synchronizer(model.getTaskIdentifier());
//            synchronizer.synchronize(context, new SynchronizerListener() {
//                public void onSynchronizerFinished(int numServicesSynced) {
////                    TaskListSubActivity.shouldRefreshTaskList = true;
//                }
//            });
        }

        cursor.close();
        cleanupTask(task.getTaskIdentifier(), repeatInfo != null);
    }

    /** Clean up state from a task. Called when deleting or completing it */
    private void cleanupTask(TaskIdentifier taskId, boolean isRepeating) {
        // delete notifications & alarms
//        ReminderService.deleteAlarm(context, null, taskId.getId());

        // delete calendar event if not repeating
        if(!isRepeating) {
            try {
                Cursor cursor = fetchTaskCursor(taskId, new String[] {
                    AbstractTaskModel.CALENDAR_URI });
                cursor.moveToFirst();
                String uri = cursor.getString(0);
                cursor.close();
                if(uri != null && uri.length() > 0) {
                    ContentResolver cr = context.getContentResolver();
                    cr.delete(Uri.parse(uri), null, null);
                    ContentValues values = new ContentValues();
                    values.put(AbstractTaskModel.CALENDAR_URI, (String)null);
                    database.update(tasksTable, values, KEY_ROWID + "=" +
                            taskId.getId(), null);
                }
            } catch (Exception e) {
                Log.e("astrid", "Error deleting calendar event", e);
            }
        }
    }

    /** Set last notification date */
    public boolean setLastNotificationTime(TaskIdentifier taskId, Date date) {
        ContentValues values = new ContentValues();
        values.put(AbstractTaskModel.LAST_NOTIFIED, date.getTime());
        return database.update(tasksTable, values,
                KEY_ROWID + "=" + taskId.getId(), null) > 0;
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
        Cursor cursor = fetchTaskCursor(taskId, TaskModelForEdit.FIELD_LIST);
        if(cursor == null)
            return null;
        activity.startManagingCursor(cursor);
        TaskModelForEdit model = new TaskModelForEdit(taskId, cursor);
        return model;
    }

    /** Returns a TaskModelForList corresponding to the given TaskIdentifier */
    public TaskModelForList fetchTaskForList(TaskIdentifier taskId) throws SQLException {
        Cursor cursor = fetchTaskCursor(taskId, TaskModelForList.FIELD_LIST);
        if(cursor == null)
            return null;
        TaskModelForList model = new TaskModelForList(cursor);
        cursor.close();
        return model;
    }

    /** Returns a TaskModelForXml corresponding to the given TaskIdentifier */
    public TaskModelForXml fetchTaskForXml(TaskIdentifier taskId) throws SQLException {
        Cursor cursor = fetchTaskCursor(taskId, TaskModelForXml.FIELD_LIST);
        if(cursor == null)
            return null;
        TaskModelForXml model = new TaskModelForXml(cursor);
        cursor.close();
        return model;
    }

    /* Attempts to return a TaskModelForXml for the given name and creation date */
    public TaskModelForXml fetchTaskForXml(String name, Date creationDate) {
        Cursor cursor;
        try {
            cursor = fetchTaskCursor(name, creationDate.getTime(),
                    TaskModelForXml.FIELD_LIST);
        } catch (SQLException e) {
            return null;
        }
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        TaskModelForXml model = new TaskModelForXml(cursor);
        cursor.close();
        return model;
    }

    /** Returns a TaskModelForReminder corresponding to the given TaskIdentifier */
    public TaskModelForReminder fetchTaskForReminder(TaskIdentifier taskId) throws SQLException {
        Cursor cursor = fetchTaskCursor(taskId, TaskModelForReminder.FIELD_LIST);
        TaskModelForReminder model = new TaskModelForReminder(cursor);
        cursor.close();
        return model;
    }

    /** Returns a TaskModelForSync corresponding to the given TaskIdentifier */
    public TaskModelForSync fetchTaskForSync(TaskIdentifier taskId) throws SQLException {
        Cursor cursor = fetchTaskCursor(taskId, TaskModelForSync.FIELD_LIST);
        TaskModelForSync model = new TaskModelForSync(cursor);
        cursor.close();
        return model;
    }

    /** Returns a TaskModelForView by name */
    public TaskModelForSync searchForTaskForSync(String name) throws SQLException {
        Cursor cursor = database.query(true, tasksTable, TaskModelForSync.FIELD_LIST,
                AbstractTaskModel.NAME + " = ? AND " +
                    AbstractTaskModel.PROGRESS_PERCENTAGE + " < "+
                        AbstractTaskModel.COMPLETE_PERCENTAGE,
                new String[] { name }, null, null, null, null);
        try {
            if (cursor == null || cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            TaskModelForSync model = new TaskModelForSync(cursor);
            return model;
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    /** Returns a TaskModelForView corresponding to the given TaskIdentifier */
    public TaskModelForNotify fetchTaskForNotify(TaskIdentifier taskId) throws SQLException {
        Cursor cursor = fetchTaskCursor(taskId, TaskModelForNotify.FIELD_LIST);
        TaskModelForNotify model = new TaskModelForNotify(cursor);
        cursor.close();
        return model;
    }

    /** Moves cursor to the task.
     * Don't forget to close the cursor when you're done. */
    private Cursor fetchTaskCursor(TaskIdentifier taskId, String[] fieldList) {
        long id = taskId.getId();
        Cursor cursor = database.query(true, tasksTable, fieldList,
                KEY_ROWID + "=" + id, null, null, null, null, null);
        if (cursor == null)
            throw new SQLException("Returned empty set!");

        cursor.moveToFirst();
        return cursor;
    }

    /** Returns null if unsuccessful, otherwise moves cursor to the task.
     * Don't forget to close the cursor when you're done. */
    private Cursor fetchTaskCursor(String name, long creationDate, String[] fieldList) {
        // truncate millis
        final String where = AbstractTaskModel.NAME + " = ? AND "
                + AbstractTaskModel.CREATION_DATE + " LIKE ?";

        String approximateCreationDate = (creationDate / 1000) + "%";
        Cursor cursor = database.query(true, tasksTable, fieldList,
                where, new String[] {name, approximateCreationDate}, null, null, null, null);
        if (cursor == null)
            throw new SQLException("Returned empty set!");

        if (cursor.moveToFirst()) {
            return cursor;
        }
        cursor.close();
        return null;
    }
    // --- methods supporting individual features

    /** Returns a TaskModelForView corresponding to the given TaskIdentifier */
    public int fetchTaskPostponeCount(TaskIdentifier taskId) throws SQLException {
        Cursor cursor = fetchTaskCursor(taskId, new String[] {AbstractTaskModel.POSTPONE_COUNT});
        try {
            if (cursor == null || cursor.getCount() == 0)
                return 0;
            cursor.moveToFirst();
            return cursor.getInt(0);
        } catch (Exception e) {
            return 0;
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    /** Updates the alarm for the task identified by the given id
     * @param taskId */
    public void updateAlarmForTask(TaskIdentifier taskId) throws SQLException {
//        TaskModelForNotify task = fetchTaskForNotify(taskId);
        AlertController alertController = new AlertController(context);
        alertController.open();
//        ReminderService.updateAlarm(context, this, alertController, task);
        alertController.close();
    }

    public ArrayList<TaskModelForWidget> getTasksForWidget(String limit) {

    	Cursor cursor = database.query(tasksTable, TaskModelForWidget.FIELD_LIST,
    	        AbstractTaskModel.PROGRESS_PERCENTAGE + " < " +
                AbstractTaskModel.COMPLETE_PERCENTAGE + " AND (" +
                AbstractTaskModel.HIDDEN_UNTIL + " ISNULL OR " + AbstractTaskModel.HIDDEN_UNTIL + " < " +
                System.currentTimeMillis() + ")", null, null, null,
                AbstractTaskModel.IMPORTANCE + " * " + (5 * 24 * 3600 * 1000L) +
                    " + CASE WHEN MAX(pdd, ddd) = 0 THEN " +
                    (System.currentTimeMillis() + (7 * 24 * 3600 * 1000L)) +
                    " ELSE (CASE WHEN pdd = 0 THEN ddd ELSE pdd END) END ASC", limit);

    	try {
            ArrayList<TaskModelForWidget> list = new ArrayList<TaskModelForWidget>();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                list.add(new TaskModelForWidget(cursor));
            return list;
    	} finally {
    	    cursor.close();
    	}
    }

    public ArrayList<TaskModelForProvider> getTasksForProvider(String limit) {

    	Cursor cursor = database.query(tasksTable, TaskModelForProvider.FIELD_LIST,
    	        AbstractTaskModel.PROGRESS_PERCENTAGE + " < " +
                AbstractTaskModel.COMPLETE_PERCENTAGE + " AND (" +
                AbstractTaskModel.HIDDEN_UNTIL + " ISNULL OR " + AbstractTaskModel.HIDDEN_UNTIL + " < " +
                System.currentTimeMillis() + ")", null, null, null,
                AbstractTaskModel.IMPORTANCE + " * " + (5 * 24 * 3600 * 1000L) +
                    " + CASE WHEN MAX(pdd, ddd) = 0 THEN " +
                    (System.currentTimeMillis() + (7 * 24 * 3600 * 1000L)) +
                    " ELSE (CASE WHEN pdd = 0 THEN ddd ELSE pdd END) END ASC", limit);

    	try {
            ArrayList<TaskModelForProvider> list = new ArrayList<TaskModelForProvider>();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                list.add(new TaskModelForProvider(cursor));
            return list;
    	} finally {
    	    cursor.close();
    	}
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
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    @Override
    public synchronized void open() throws SQLException {
        SQLiteOpenHelper databaseHelper = new TaskModelDatabaseHelper(
                context, tasksTable, tasksTable);
        database = databaseHelper.getWritableDatabase();
    }

    /** Closes database resource */
    @Override
    public void close() {
        database.close();
    }
}
