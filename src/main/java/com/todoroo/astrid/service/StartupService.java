/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;

import org.tasks.Broadcaster;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.io.File;

import javax.inject.Inject;

import timber.log.Timber;

public class StartupService {

    public static final int V4_8_0 = 380;

    private final Context context;
    private final Database database;
    private final Preferences preferences;
    private final TaskDeleter taskDeleter;
    private final Broadcaster broadcaster;
    private final Tracker tracker;

    @Inject
    public StartupService(@ForApplication Context context, Database database, Preferences preferences, TaskDeleter taskDeleter,
                          Broadcaster broadcaster, Tracker tracker) {
        this.context = context;
        this.database = database;
        this.preferences = preferences;
        this.taskDeleter = taskDeleter;
        this.broadcaster = broadcaster;
        this.tracker = tracker;
    }

    /** Called when this application is started up */
    public synchronized void onStartupApplication() {
        try {
            database.openForWriting();
        } catch (SQLiteException e) {
            tracker.reportException(e);
            return;
        }

        // read current version
        final int lastVersion = preferences.getLastSetVersion();
        final int currentVersion = BuildConfig.VERSION_CODE;

        Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion);

        // invoke upgrade service
        if(lastVersion != currentVersion) {
            new Thread() {
                @Override
                public void run() {
                    upgrade(lastVersion, currentVersion);
                }
            }.start();
            preferences.setDefaults();
        }

        // perform startup activities in a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                taskDeleter.deleteTasksWithEmptyTitles(null);
            }
        }).start();

        if (lastVersion == 0) {
            broadcaster.firstLaunch();
        }
    }

    private void upgrade(int from, int to) {
        try {
            if (from > 0) {
                if (from < V4_8_0) {
                    performMarshmallowMigration();
                }
                tracker.reportEvent(Tracking.Events.UPGRADE, Integer.toString(from));
            }
            preferences.setCurrentVersion(to);
        } finally {
            context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
        }
    }

    private void performMarshmallowMigration() {
        try {
            // preserve pre-marshmallow default backup location
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (!preferences.isStringValueSet(R.string.p_backup_dir)) {
                    String directory = String.format("%s/astrid",
                            Environment.getExternalStorageDirectory());
                    File file = new File(directory);
                    if (file.exists() && file.isDirectory()) {
                        preferences.setString(R.string.p_backup_dir, directory);
                    }
                }
            }
        } catch (Exception e) {
            tracker.reportException(e);
        }
    }
}
