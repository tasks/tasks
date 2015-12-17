/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;

import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.DatabaseUpdateListener;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.tags.TaskToTagMetadata;

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

    private final TagDataDao tagDataDao;
    private final Database database;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final GtasksSyncService gtasksSyncService;
    private final MetadataDao metadataDao;
    private final Preferences preferences;
    private final TasksXmlImporter xmlImporter;
    private final TaskDeleter taskDeleter;
    private final Broadcaster broadcaster;
    private final DialogBuilder dialogBuilder;

    @Inject
    public StartupService(TagDataDao tagDataDao, Database database, GtasksPreferenceService gtasksPreferenceService,
                          GtasksSyncService gtasksSyncService, MetadataDao metadataDao,
                          Preferences preferences, TasksXmlImporter xmlImporter,
                          TaskDeleter taskDeleter, Broadcaster broadcaster, DialogBuilder dialogBuilder) {
        this.tagDataDao = tagDataDao;
        this.database = database;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksSyncService = gtasksSyncService;
        this.metadataDao = metadataDao;
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

        database.addListener(new DatabaseUpdateListener() {
            @Override
            public void onDatabaseUpdated() {
                Astrid2TaskProvider.notifyDatabaseModification(activity);
                Astrid3ContentProvider.notifyDatabaseModification(activity);
            }
        });

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
                }}, AstridActivity.REQUEST_UPGRADE);
            }
            preferences.setDefaults();
            preferences.setCurrentVersion(currentVersion);
        }

        initializeDatabaseListeners();

        // perform startup activities in a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                taskDeleter.deleteTasksWithEmptyTitles(null);

                // if sync ongoing flag was set, clear it
                gtasksPreferenceService.stopOngoing();

                gtasksSyncService.initialize();
            }
        }).start();

        if (lastVersion == 0) {
            broadcaster.firstLaunch();
        }

        hasStartedUp = true;
    }

    private void initializeDatabaseListeners() {
        // This listener makes sure that when a tag's name is created or changed,
        // the corresponding metadata will also update
        tagDataDao.addListener(new ModelUpdateListener<TagData>() {
            @Override
            public void onModelUpdated(TagData model) {
                ContentValues values = model.getSetValues();
                Metadata m = new Metadata();
                if (values != null) {
                    if (values.containsKey(TagData.NAME.name)) {
                        m.setValue(TaskToTagMetadata.TAG_NAME, model.getName());
                        metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                                TaskToTagMetadata.TAG_UUID.eq(model.getUUID())), m);
                    }
                }
            }
        });
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
