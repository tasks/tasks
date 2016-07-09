/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteException;

import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.dao.Database;

import org.tasks.Broadcaster;
import org.tasks.BuildConfig;
import org.tasks.analytics.Tracker;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Service which handles jobs that need to be run when Astrid starts up.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class StartupService {

    // --- application startup

    private final Database database;
    private final Preferences preferences;
    private final TaskDeleter taskDeleter;
    private final Broadcaster broadcaster;
    private final Tracker tracker;

    @Inject
    public StartupService(Database database, Preferences preferences, TaskDeleter taskDeleter,
                          Broadcaster broadcaster, Tracker tracker) {
        this.database = database;
        this.preferences = preferences;
        this.taskDeleter = taskDeleter;
        this.broadcaster = broadcaster;
        this.tracker = tracker;
    }

    /**
     * bit to prevent multiple initializations
     */
    private static boolean hasStartedUp = false;

    /** Called when this application is started up */
    public synchronized void onStartupApplication(final Activity activity) {
        if(hasStartedUp || activity == null) {
            return;
        }

        try {
            database.openForWriting();
        } catch (SQLiteException e) {
            Timber.e(e, e.getMessage());
            tracker.reportException(e);
            return;
        }

        // read current version
        final int lastVersion = preferences.getLastSetVersion();
        int currentVersion = BuildConfig.VERSION_CODE;

        Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion);

        // invoke upgrade service
        if(lastVersion != currentVersion) {
            if(lastVersion > 0) {
                activity.startActivityForResult(new Intent(activity, UpgradeActivity.class) {{
                    putExtra(UpgradeActivity.TOKEN_FROM_VERSION, lastVersion);
                }}, TaskListActivity.REQUEST_UPGRADE);
            }
            preferences.setDefaults();
            preferences.setCurrentVersion(currentVersion);
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

        hasStartedUp = true;
    }
}
