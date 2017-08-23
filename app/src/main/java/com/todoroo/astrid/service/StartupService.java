/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.BackgroundScheduler;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class StartupService {

    private static final int V4_8_0 = 380;
    private static final int V4_9_5 = 434;

    private final Database database;
    private final Preferences preferences;
    private final TaskDeleter taskDeleter;
    private final Tracker tracker;
    private final TagDataDao tagDataDao;
    private final TagService tagService;
    private final MetadataDao metadataDao;
    private final LocalBroadcastManager localBroadcastManager;
    private final Context context;

    @Inject
    public StartupService(Database database, Preferences preferences, TaskDeleter taskDeleter,
                          Tracker tracker, TagDataDao tagDataDao, TagService tagService,
                          MetadataDao metadataDao, LocalBroadcastManager localBroadcastManager,
                          @ForApplication Context context) {
        this.database = database;
        this.preferences = preferences;
        this.taskDeleter = taskDeleter;
        this.tracker = tracker;
        this.tagDataDao = tagDataDao;
        this.tagService = tagService;
        this.metadataDao = metadataDao;
        this.localBroadcastManager = localBroadcastManager;
        this.context = context;
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

        BackgroundScheduler.enqueueWork(context);

        // perform startup activities in a background thread
        new Thread(() -> taskDeleter.deleteTasksWithEmptyTitles(null)).start();
    }

    private void upgrade(int from, int to) {
        try {
            if (from > 0) {
                if (from < V4_8_0) {
                    performMarshmallowMigration();
                }
                if (from < V4_9_5) {
                    removeDuplicateTags();
                }
                tracker.reportEvent(Tracking.Events.UPGRADE, Integer.toString(from));
            }
            preferences.setCurrentVersion(to);
        } finally {
            localBroadcastManager.broadcastRefresh();
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

    private void removeDuplicateTags() {
        ListMultimap<String, TagData> tagsByUuid = Multimaps.index(tagService.getTagList(), TagData::getUuid);
        for (String uuid : tagsByUuid.keySet()) {
            removeDuplicateTagData(tagsByUuid.get(uuid));
            removeDuplicateTagMetadata(uuid);
        }
        localBroadcastManager.broadcastRefresh();
    }

    private void removeDuplicateTagData(List<TagData> tagData) {
        for (int i = 1 ; i < tagData.size() ; i++) {
            tagDataDao.delete(tagData.get(i).getId());
        }
    }

    private void removeDuplicateTagMetadata(String uuid) {
        Criterion fullCriterion = Criterion.and(
                Metadata.KEY.eq(TaskToTagMetadata.KEY),
                TaskToTagMetadata.TAG_UUID.eq(uuid),
                Metadata.DELETION_DATE.eq(0));
        List<Metadata> metadatas = metadataDao.toList(fullCriterion);
        ImmutableListMultimap<Long, Metadata> metadataByTask = Multimaps.index(metadatas, Metadata::getTask);
        for (Long key : metadataByTask.keySet()) {
            ImmutableList<Metadata> tagData = metadataByTask.get(key);
            for (int i = 1 ; i < tagData.size() ; i++) {
                metadataDao.delete(tagData.get(i).getId());
            }
        }
    }
}
