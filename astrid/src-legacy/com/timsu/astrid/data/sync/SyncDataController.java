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
package com.timsu.astrid.data.sync;

import java.util.HashSet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.sync.SyncMapping.SyncMappingDatabaseHelper;
import com.timsu.astrid.data.task.AbstractTaskModel;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForSync;

/** Controller for Tag-related operations */
public class SyncDataController extends AbstractController {

    private SQLiteDatabase syncDatabase;


    // --- updated tasks list

    /** Mark all updated tasks as finished synchronizing */
    public boolean clearUpdatedTaskList(int syncServiceId) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(SyncMapping.UPDATED, 0);
        return syncDatabase.update(syncTable, values,
                SyncMapping.SYNC_SERVICE + " = " + syncServiceId, null) > 0;
    }

    /** Indicate that this task's properties were updated */
    public boolean addToUpdatedList(TaskIdentifier taskId) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(SyncMapping.UPDATED, 1);
        return syncDatabase.update(syncTable, values,
                SyncMapping.TASK + " = " + taskId.getId(), null) > 0;
    }

    public static void taskUpdated(Context context, AbstractTaskModel task) {
        if(!(task instanceof TaskModelForSync)) {
            SyncDataController syncController = new SyncDataController(context);
            syncController.open();
            syncController.addToUpdatedList(task.getTaskIdentifier());
            syncController.close();
        }
    }

    // --- sync mapping

    /** Get all mappings for the given synchronization service */
    public HashSet<SyncMapping> getSyncMappings(int syncServiceId) throws SQLException {
        HashSet<SyncMapping> list = new HashSet<SyncMapping>();
        Cursor cursor = syncDatabase.query(syncTable,
                SyncMapping.FIELD_LIST,
                SyncMapping.SYNC_SERVICE + " = " + syncServiceId,
                null, null, null, null);

        try {
            if(cursor.getCount() == 0)
                return list;
            do {
                cursor.moveToNext();
                list.add(new SyncMapping(cursor));
            } while(!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /** Get all mappings for specified task for all synchronization services */
    public HashSet<SyncMapping> getSyncMappings(TaskIdentifier taskId)
            throws SQLException {
        HashSet<SyncMapping> list = new HashSet<SyncMapping>();
        Cursor cursor = syncDatabase.query(syncTable,
                SyncMapping.FIELD_LIST,
                SyncMapping.TASK + " = ?",
                new String[] { "" + taskId.getId() },
                null, null, null);

        try {
            if(cursor.getCount() == 0)
                return list;
            do {
                cursor.moveToNext();
                list.add(new SyncMapping(cursor));
            } while(!cursor.isLast());

            return list;
        } finally {
            cursor.close();
        }
    }

    /** Get mapping for given task */
    public SyncMapping getSyncMapping(int syncServiceId, TaskIdentifier taskId)
            throws SQLException {
        Cursor cursor = syncDatabase.query(syncTable,
                SyncMapping.FIELD_LIST,
                SyncMapping.SYNC_SERVICE + " = ? AND " +
                    SyncMapping.TASK + " = ?",
                new String[] { "" + syncServiceId, "" + taskId.getId() },
                null, null, null);

        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToNext();
            return new SyncMapping(cursor);
        } finally {
            cursor.close();
        }
    }

    /** Saves the given task to the database. Returns true on success. */
    public boolean saveSyncMapping(SyncMapping mapping) {
        long newRow = syncDatabase.insert(syncTable, SyncMapping.TASK,
                mapping.getMergedValues());

        mapping.setId(newRow);

        return newRow >= 0;
    }

    /** Deletes the given mapping. Returns true on success */
    public boolean deleteSyncMapping(SyncMapping mapping) {
        // was never saved
        if(mapping.getId() == 0)
            return false;

        return syncDatabase.delete(syncTable, KEY_ROWID + "=" +
                mapping.getId(), null) > 0;
    }

    /** Deletes the given mapping. Returns true on success */
    public boolean deleteAllMappings(int syncServiceId) {
        return syncDatabase.delete(syncTable, SyncMapping.SYNC_SERVICE +
                "=" + syncServiceId, null) > 0;
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
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    @Override
    public synchronized void open() throws SQLException {
        SQLiteOpenHelper helper = new SyncMappingDatabaseHelper(context,
                syncTable, syncTable);
        syncDatabase = helper.getWritableDatabase();
    }

    /** Closes database resource */
    @Override
    public void close() {
        if(syncDatabase != null)
            syncDatabase.close();
    }
}
