/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.sync.SyncMapping.SyncMappingDatabaseHelper;
import com.timsu.astrid.data.task.AbstractTaskModel;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForSync;

import java.util.HashSet;

/**
 * Controller for Tag-related operations
 */

public class SyncDataController extends LegacyAbstractController {

    private SQLiteDatabase syncDatabase;


    // --- updated tasks list

    /**
     * Indicate that this task's properties were updated
     */
    public void addToUpdatedList(TaskIdentifier taskId) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(SyncMapping.UPDATED, 1);
        syncDatabase.update(syncTable, values, SyncMapping.TASK + " = " + taskId.getId(), null);
    }

    public static void taskUpdated(Context context, AbstractTaskModel task) {
        if (!(task instanceof TaskModelForSync)) {
            SyncDataController syncController = new SyncDataController(context);
            syncController.open();
            syncController.addToUpdatedList(task.getTaskIdentifier());
            syncController.close();
        }
    }

    // --- sync mapping

    /**
     * Get all mappings for the given synchronization service
     */
    public HashSet<SyncMapping> getSyncMappings(int syncServiceId) throws SQLException {
        HashSet<SyncMapping> list = new HashSet<SyncMapping>();
        Cursor cursor = syncDatabase.query(syncTable,
                SyncMapping.FIELD_LIST,
                SyncMapping.SYNC_SERVICE + " = " + syncServiceId,
                null, null, null, null);

        try {
            if (cursor.getCount() == 0) {
                return list;
            }
            do {
                cursor.moveToNext();
                list.add(new SyncMapping(cursor));
            } while (!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /**
     * Get all mappings for specified task for all synchronization services
     */
    public HashSet<SyncMapping> getSyncMappings(TaskIdentifier taskId)
            throws SQLException {
        HashSet<SyncMapping> list = new HashSet<SyncMapping>();
        Cursor cursor = syncDatabase.query(syncTable,
                SyncMapping.FIELD_LIST,
                SyncMapping.TASK + " = ?",
                new String[]{"" + taskId.getId()},
                null, null, null);

        try {
            if (cursor.getCount() == 0) {
                return list;
            }
            do {
                cursor.moveToNext();
                list.add(new SyncMapping(cursor));
            } while (!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /**
     * Get mapping for given task
     */
    public SyncMapping getSyncMapping(int syncServiceId, TaskIdentifier taskId)
            throws SQLException {
        Cursor cursor = syncDatabase.query(syncTable,
                SyncMapping.FIELD_LIST,
                SyncMapping.SYNC_SERVICE + " = ? AND " +
                        SyncMapping.TASK + " = ?",
                new String[]{"" + syncServiceId, "" + taskId.getId()},
                null, null, null);

        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToNext();
            return new SyncMapping(cursor);
        } finally {
            cursor.close();
        }
    }

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public SyncDataController(Context context) {
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
        SQLiteOpenHelper helper = new SyncMappingDatabaseHelper(context,
                syncTable, syncTable);
        syncDatabase = helper.getWritableDatabase();
    }

    /**
     * Closes database resource
     */
    @Override
    public void close() {
        if (syncDatabase != null) {
            syncDatabase.close();
        }
    }
}
