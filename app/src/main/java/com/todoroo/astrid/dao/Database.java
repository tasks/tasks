/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.RoomDatabase;
import android.database.Cursor;

import com.todoroo.astrid.data.Task;

import org.tasks.data.Alarm;
import org.tasks.data.AlarmDao;
import org.tasks.data.Filter;
import org.tasks.data.FilterDao;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskAttachment;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
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
                Task.class,
                Alarm.class,
                Location.class,
                Tag.class,
                GoogleTask.class,
                Filter.class,
                GoogleTaskList.class
        },
        version = 53)
public abstract class Database extends RoomDatabase {

    public abstract NotificationDao notificationDao();
    public abstract TagDataDao getTagDataDao();
    public abstract UserActivityDao getUserActivityDao();
    public abstract TaskAttachmentDao getTaskAttachmentDao();
    public abstract TaskListMetadataDao getTaskListMetadataDao();
    public abstract AlarmDao getAlarmDao();
    public abstract LocationDao getLocationDao();
    public abstract TagDao getTagDao();
    public abstract GoogleTaskDao getGoogleTaskDao();
    public abstract FilterDao getFilterDao();
    public abstract GoogleTaskListDao getGoogleTaskListDao();
    public abstract TaskDao getTaskDao();

    public static final String NAME = "database";

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

    void onDatabaseUpdated() {
        if (onDatabaseUpdated != null) {
            onDatabaseUpdated.run();
        }
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
}

