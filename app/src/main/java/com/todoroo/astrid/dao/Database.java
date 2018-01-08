/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.RoomDatabase;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.UserActivity;

import org.tasks.notifications.Notification;
import org.tasks.notifications.NotificationDao;

import java.io.IOException;

import timber.log.Timber;

/**
 * Database wrapper
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@android.arch.persistence.room.Database(
        entities = {
                Notification.class,
                TagData.class,
                UserActivity.class,
                TaskAttachment.class,
                TaskListMetadata.class,
                StoreObject.class,
                Task.class,
                Metadata.class
        },
        version = 46)
public abstract class Database extends RoomDatabase {

    public abstract NotificationDao notificationDao();
    public abstract TagDataDao getTagDataDao();
    public abstract UserActivityDao getUserActivityDao();
    public abstract TaskAttachmentDao getTaskAttachmentDao();
    public abstract TaskListMetadataDao getTaskListMetadataDao();
    public abstract StoreObjectDao getStoreObjectDao();

    public static final String NAME = "database";

    public static final Table[] TABLES =  new Table[] {
            Task.TABLE,
            Metadata.TABLE
    };

    private SupportSQLiteDatabase database;
    private Runnable onDatabaseUpdated;

    // --- implementation

    public String getName() {
        return NAME;
    }

    public Database setOnDatabaseUpdated(Runnable onDatabaseUpdated) {
        this.onDatabaseUpdated = onDatabaseUpdated;
        return this;
    }

    private void onDatabaseUpdated() {
        if (onDatabaseUpdated != null) {
            onDatabaseUpdated.run();
        }
    }

    /**
     * Return the name of the table containing these models
     */
    public final Table getTable(Class<? extends AbstractModel> modelType) {
        for(Table table : TABLES) {
            if(table.modelClass.equals(modelType)) {
                return table;
            }
        }
        throw new UnsupportedOperationException("Unknown model class " + modelType); //$NON-NLS-1$
    }

    /**
     * Open the database for writing. Must be closed afterwards. If user is
     * out of disk space, database may be opened for reading instead
     */
    public synchronized final void openForWriting() {
        if(database != null && !database.isReadOnly() && database.isOpen()) {
            return;
        }

        try {
            database = getOpenHelper().getWritableDatabase();
        } catch (NullPointerException e) {
            Timber.e(e, e.getMessage());
            throw new IllegalStateException(e);
        } catch (final RuntimeException original) {
            Timber.e(original, original.getMessage());
            try {
                // provide read-only database
                openForReading();
            } catch (Exception readException) {
                Timber.e(readException, readException.getMessage());
                // throw original write exception
                throw original;
            }
        }
    }

    /**
     * Open the database for reading. Must be closed afterwards
     */
    public synchronized final void openForReading() {
        if(database != null && database.isOpen()) {
            return;
        }
        database = getOpenHelper().getReadableDatabase();
    }

    /**
     * Close the database if it has been opened previously
     */
    public synchronized final void close() {
        if(database != null) {
            try {
                database.close();
            } catch (IOException e) {
                Timber.e(e, e.getMessage());
            }
        }
        database = null;
    }

    /**
     * @return sql database. opens database if not yet open
     */
    public synchronized final SupportSQLiteDatabase getDatabase() {
        if(database == null) {
            AndroidUtilities.sleepDeep(300L);
            openForWriting();
        }
        return database;
    }

    /**
     * @return human-readable database name for debugging
     */
    @Override
    public String toString() {
        return "DB:" + getName();
    }

    // --- database wrapper

    public Cursor rawQuery(String sql) {
        return getDatabase().query(sql, null);
    }

    public long insert(String table, ContentValues values) {
        long result;
        try {
            result = getDatabase().insert(table, SQLiteDatabase.CONFLICT_REPLACE, values);
        } catch (SQLiteConstraintException e) { // Throw these exceptions
            throw e;
        } catch (Exception e) { // Suppress others
            Timber.e(e, e.getMessage());
            result = -1;
        }
        onDatabaseUpdated();
        return result;
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        int result = getDatabase().delete(table, whereClause, whereArgs);
        onDatabaseUpdated();
        return result;
    }

    public int update(String table, ContentValues  values, String whereClause) {
        int result = getDatabase().update(table, SQLiteDatabase.CONFLICT_REPLACE, values, whereClause, null);
        onDatabaseUpdated();
        return result;
    }
}

