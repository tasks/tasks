/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.alerts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.alerts.Alert.AlertDatabaseHelper;
import com.timsu.astrid.data.task.TaskIdentifier;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Controller for Tag-related operations
 */

public class AlertController extends LegacyAbstractController {

    private SQLiteDatabase alertDatabase;

    /**
     * Get a list of alerts for the given task
     */
    public List<Date> getTaskAlerts(TaskIdentifier
                                            taskId) throws SQLException {
        List<Date> list = new LinkedList<Date>();
        Cursor cursor = alertDatabase.query(alertsTable,
                Alert.FIELD_LIST, Alert.TASK + " = ?",
                new String[]{taskId.idAsString()}, null, null, null);

        try {
            if (cursor.getCount() == 0) {
                return list;
            }
            do {
                cursor.moveToNext();
                list.add(new Alert(cursor).getDate());
            } while (!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /**
     * Remove all alerts from the task
     */
    public void removeAlerts(TaskIdentifier taskId) throws SQLException {
        alertDatabase.delete(alertsTable,
                String.format("%s = ?", Alert.TASK),
                new String[]{taskId.idAsString()});
    }

    /**
     * Add the given tag to the task
     */
    public void addAlert(TaskIdentifier taskId, Date date) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(Alert.DATE, date.getTime());
        values.put(Alert.TASK, taskId.getId());
        alertDatabase.insert(alertsTable, Alert.TASK, values);
    }

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public AlertController(Context context) {
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
    public void open() throws SQLException {
        alertDatabase = new AlertDatabaseHelper(context,
                alertsTable, alertsTable).getWritableDatabase();
    }

    /**
     * Closes database resource
     */
    @Override
    public void close() {
        alertDatabase.close();
    }
}
