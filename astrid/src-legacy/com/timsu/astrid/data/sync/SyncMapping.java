/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.LegacyAbstractModel;
import com.timsu.astrid.data.task.TaskIdentifier;


/** A single tag on a task */
@SuppressWarnings("nls")
public class SyncMapping extends LegacyAbstractModel {


    /** Version number of this model */
    static final int                   VERSION             = 1;

    // field names

    static final String                TASK          = "task";
    static final String                SYNC_SERVICE  = "service";
    static final String                REMOTE_ID     = "remoteId";
    static final String                UPDATED       = "updated";

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();
    static {
        defaultValues.put(UPDATED, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
        TASK,
        SYNC_SERVICE,
        REMOTE_ID,
        UPDATED,
    };

    // --- database helper

    /** Database Helper manages creating new tables and updating old ones */
    static class SyncMappingDatabaseHelper extends SQLiteOpenHelper {
        String tableName;
        Context context;

        SyncMappingDatabaseHelper(Context context, String databaseName, String tableName) {
            super(context, databaseName, null, VERSION);
            this.tableName = tableName;
            this.context = context;
        }

        @Override
        public synchronized void onCreate(SQLiteDatabase db) {
            String sql = new StringBuilder().
            append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (").
                append(LegacyAbstractController.KEY_ROWID).append(" integer primary key autoincrement, ").
                append(TASK).append(" integer not null,").
                append(SYNC_SERVICE).append(" integer not null,").
                append(REMOTE_ID).append(" text not null,").
                append(UPDATED).append(" integer not null,").
                append("unique (").append(TASK).append(",").append(SYNC_SERVICE).append(")").
            append(");").toString();
            db.execSQL(sql);
        }

        @Override
        public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(getClass().getSimpleName(), "Upgrading database from version " +
                    oldVersion + " to " + newVersion + ".");

            switch(oldVersion) {
            default:
                // we don't know how to handle it... show an error
                Log.e(getClass().getSimpleName(), "Unsupported migration from " + oldVersion + " to " + newVersion);

            }
        }
    }


    // --- constructor pass-through
//
//    public SyncMapping(TaskIdentifier task, TaskProxy taskProxy) {
//        this(task, taskProxy.getSyncServiceId(), taskProxy.getRemoteId());
//    }

    public SyncMapping(TaskIdentifier task, int syncServiceId, String remoteId) {
        super();
        setTask(task);
        setSyncServiceId(syncServiceId);
        setRemoteId(remoteId);
    }

    SyncMapping(Cursor cursor) {
        super(cursor);
        getId();
        getTask();
        getSyncServiceId();
        getRemoteId();
        isUpdated();
    }

    // --- getters and setters

    public void setId(long id) {
        putIfChangedFromDatabase(LegacyAbstractController.KEY_ROWID, id);
    }

    public long getId() {
        try {
            return retrieveLong(LegacyAbstractController.KEY_ROWID);
        } catch (UnsupportedOperationException e) {
            return 0;
        }
    }

    public TaskIdentifier getTask() {
        return new TaskIdentifier(retrieveLong(TASK));
    }

    public int getSyncServiceId() {
        return retrieveInteger(SYNC_SERVICE);
    }

    public String getRemoteId() {
        return retrieveString(REMOTE_ID);
    }

    public boolean isUpdated() {
        return retrieveInteger(UPDATED) == 1;
    }

    private void setTask(TaskIdentifier task) {
        setValues.put(TASK, task.getId());
    }

    private void setSyncServiceId(int id) {
        setValues.put(SYNC_SERVICE, id);
    }

    private void setRemoteId(String remoteId) {
        setValues.put(REMOTE_ID, remoteId);
    }
}
