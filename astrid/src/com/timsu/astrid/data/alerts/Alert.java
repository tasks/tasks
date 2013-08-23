/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.alerts;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.LegacyAbstractModel;
import com.timsu.astrid.data.task.TaskIdentifier;


/** A single alert on a task */
@SuppressWarnings("nls")
public class Alert extends LegacyAbstractModel {

    /** Version number of this model */
    static final int                   VERSION             = 1;

    // field names

    public static final String                TASK                = "task";
    public static final String                DATE                = "date";

    /** Default values container */
    private static final ContentValues defaultValues       = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    static String[] FIELD_LIST = new String[] {
        LegacyAbstractController.KEY_ROWID,
        TASK,
        DATE,
    };

    // --- database helper

    /** Database Helper manages creating new tables and updating old ones */
    static class AlertDatabaseHelper extends SQLiteOpenHelper {
        String tableName;
        Context context;

        AlertDatabaseHelper(Context context, String databaseName, String tableName) {
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
                append(DATE).append(" integer not null,").
                append("unique (").append(TASK).append(",").append(DATE).append(")").
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

    Alert(TaskIdentifier task, Date date) {
        super();
        setTask(task);
        setDate(date);
    }

    public Alert(Cursor cursor) {
        super(cursor);
    }

    // --- getters and setters: expose them as you see fit

    public boolean isNew() {
        return getCursor() == null;
    }

    public TaskIdentifier getTask() {
        return new TaskIdentifier(retrieveLong(TASK));
    }

    public Date getDate() {
        return new Date(retrieveLong(DATE));
    }

    private void setTask(TaskIdentifier task) {
        setValues.put(TASK, task.getId());
    }

    private void setDate(Date date) {
        setValues.put(DATE, date.getTime());
    }
}
