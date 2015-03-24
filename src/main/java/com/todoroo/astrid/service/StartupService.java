/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;

import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.backup.BackupConstants;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.DatabaseUpdateListener;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.gcal.CalendarAlarmScheduler;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import com.todoroo.astrid.provider.Astrid3ContentProvider;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service which handles jobs that need to be run when Astrid starts up.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class StartupService {

    private static final Logger log = LoggerFactory.getLogger(StartupService.class);

    // --- application startup

    private final UpgradeService upgradeService;
    private final TagDataDao tagDataDao;
    private final Database database;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final GtasksSyncService gtasksSyncService;
    private final MetadataDao metadataDao;
    private final Preferences preferences;
    private final TasksXmlImporter xmlImporter;
    private final CalendarAlarmScheduler calendarAlarmScheduler;
    private final TaskDeleter taskDeleter;
    private Broadcaster broadcaster;

    @Inject
    public StartupService(UpgradeService upgradeService, TagDataDao tagDataDao, Database database,
                          GtasksPreferenceService gtasksPreferenceService,
                          GtasksSyncService gtasksSyncService, MetadataDao metadataDao,
                          Preferences preferences, TasksXmlImporter xmlImporter,
                          CalendarAlarmScheduler calendarAlarmScheduler, TaskDeleter taskDeleter,
                          Broadcaster broadcaster) {
        this.upgradeService = upgradeService;
        this.tagDataDao = tagDataDao;
        this.database = database;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksSyncService = gtasksSyncService;
        this.metadataDao = metadataDao;
        this.preferences = preferences;
        this.xmlImporter = xmlImporter;
        this.calendarAlarmScheduler = calendarAlarmScheduler;
        this.taskDeleter = taskDeleter;
        this.broadcaster = broadcaster;
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
            handleSQLiteError(activity, e);
            return;
        }

        // read current version
        int latestSetVersion = 0;
        try {
            latestSetVersion = preferences.getCurrentVersion();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        int version = 0;
        String versionName = "0"; //$NON-NLS-1$
        try {
            PackageManager pm = activity.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(Constants.PACKAGE, PackageManager.GET_META_DATA);
            version = pi.versionCode;
            versionName = pi.versionName;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.info("astrid", "Astrid Startup. " + latestSetVersion + //$NON-NLS-1$ //$NON-NLS-2$
                " => " + version); //$NON-NLS-1$

        databaseRestoreIfEmpty(activity);

        // invoke upgrade service
        boolean justUpgraded = latestSetVersion != version;
        if(justUpgraded && version > 0) {
            if(latestSetVersion > 0) {
                upgradeService.performUpgrade(activity, latestSetVersion);
            }
            preferences.setCurrentVersion(version);
            preferences.setCurrentVersionName(versionName);
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

        if (!preferences.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            preferences.setDefaults();
        }

        if (latestSetVersion == 0) {
            broadcaster.firstLaunch();
        }

        calendarAlarmScheduler.scheduleCalendarAlarms(activity, false); // This needs to be after set preference defaults for the purposes of ab testing

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

    public static void handleSQLiteError(Activity activity, final SQLiteException e) {
        log.error(e.getMessage(), e);
        DialogUtilities.okDialog(activity, activity.getString(R.string.DB_corrupted_title), 0, activity.getString(R.string.DB_corrupted_body));
    }

    /**
     * If database exists, no tasks but metadata, and a backup file exists, restore it
     */
    private void databaseRestoreIfEmpty(Context context) {
        try {
            if(preferences.getCurrentVersion() >= UpgradeService.V3_0_0 &&
                    !context.getDatabasePath(database.getName()).exists()) {
                // we didn't have a database! restore latest file
                File directory = BackupConstants.defaultExportDirectory();
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
            log.error(e.getMessage(), e);
        }
    }
}
