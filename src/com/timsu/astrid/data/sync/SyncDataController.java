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
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.sync.SyncMapping.SyncMappingDatabaseHelper;
import com.timsu.astrid.data.task.TaskIdentifier;

/** Controller for Tag-related operations */
public class SyncDataController extends AbstractController {

    private SQLiteDatabase syncDatabase;


    // --- updated tasks list

    /** Mark all updated tasks as finished synchronizing */
    public boolean clearUpdatedTaskList(int syncServiceId) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(SyncMapping.UPDATED, 0);
        return syncDatabase.update(SYNC_TABLE_NAME, values,
                SyncMapping.SYNC_SERVICE + " = " + syncServiceId, null) > 0;
    }

    /** Indicate that this task's properties were updated */
    public boolean addToUpdatedList(TaskIdentifier taskId) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(SyncMapping.UPDATED, 1);
        return syncDatabase.update(SYNC_TABLE_NAME, values,
                SyncMapping.TASK + " = " + taskId.getId(), null) > 0;
    }

    // --- sync mapping

    /** Get all mappings for the given synchronization service */
    public Set<SyncMapping> getSyncMapping(int syncServiceId) throws SQLException {
        Set<SyncMapping> list = new HashSet<SyncMapping>();
        Cursor cursor = syncDatabase.query(SYNC_TABLE_NAME,
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

    /** Saves the given task to the database. Returns true on success. */
    public boolean saveSyncMapping(SyncMapping mapping) {
        long newRow = syncDatabase.insert(SYNC_TABLE_NAME, SyncMapping.TASK,
                mapping.getMergedValues());

        return newRow >= 0;
    }

    /** Deletes the given mapping. Returns true on success */
    public boolean deleteSyncMapping(SyncMapping mapping) {
        return syncDatabase.delete(SYNC_TABLE_NAME, KEY_ROWID + "=" +
                mapping.getId(), null) > 0;
    }

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public SyncDataController(Context context) {
        this.context = context;
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
    public SyncDataController open() throws SQLException {
        syncDatabase = new SyncMappingDatabaseHelper(context,
                SYNC_TABLE_NAME, SYNC_TABLE_NAME).getWritableDatabase();

        return this;
    }

    /** Closes database resource */
    public void close() {
        syncDatabase.close();
    }
}
