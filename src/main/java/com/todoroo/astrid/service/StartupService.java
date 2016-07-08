/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.dao.Database;

import org.tasks.Broadcaster;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;

import java.io.File;

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
    private final TasksXmlImporter xmlImporter;
    private final TaskDeleter taskDeleter;
    private final Broadcaster broadcaster;
    private final DialogBuilder dialogBuilder;

    @Inject
    public StartupService(Database database, Preferences preferences, TasksXmlImporter xmlImporter,
                          TaskDeleter taskDeleter, Broadcaster broadcaster, DialogBuilder dialogBuilder) {
        this.database = database;
        this.preferences = preferences;
        this.xmlImporter = xmlImporter;
        this.taskDeleter = taskDeleter;
        this.broadcaster = broadcaster;
        this.dialogBuilder = dialogBuilder;
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
            dialogBuilder.newMessageDialog(R.string.DB_corrupted_body)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        // read current version
        final int lastVersion = preferences.getLastSetVersion();
        int currentVersion = BuildConfig.VERSION_CODE;

        Timber.i("Astrid Startup. %s => %s", lastVersion, currentVersion);

        databaseRestoreIfEmpty(activity);

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

    /**
     * If database exists, no tasks but metadata, and a backup file exists, restore it
     */
    private void databaseRestoreIfEmpty(Context context) {
        try {
            if(preferences.getLastSetVersion() > UpgradeActivity.V3_0_0 &&
                    !context.getDatabasePath(database.getName()).exists()) {
                // we didn't have a database! restore latest file
                File directory = preferences.getBackupDirectory();
                if(!directory.exists()) {
                    return;
                }
                File[] children = directory.listFiles();
                AndroidUtilities.sortFilesByDateDesc(children);
                if(children.length > 0) {
                    xmlImporter.importTasks(context, children[0].getAbsolutePath(), null);
                }
            }
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
    }
}
