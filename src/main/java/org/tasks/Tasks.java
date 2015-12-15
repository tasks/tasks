package org.tasks;

import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.analytics.Tracker;
import org.tasks.filters.FilterCounter;
import org.tasks.injection.InjectingApplication;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.RefreshScheduler;
import org.tasks.sync.SyncThrottle;

import javax.inject.Inject;

@SuppressWarnings("UnusedDeclaration")
public class Tasks extends InjectingApplication {

    @Inject Database database;
    @Inject TaskDao taskDao;
    @Inject MetadataDao metadataDao;
    @Inject TagDataDao tagDataDao;
    @Inject StoreObjectDao storeObjectDao;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject TaskService taskService;
    @Inject UpgradeService upgradeService;
    @Inject SyncV2Service syncV2Service;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksSyncService gtasksSyncService;
    @Inject TagService tagService;
    @Inject Broadcaster broadcaster;
    @Inject FilterCounter filterCounter;
    @Inject RefreshScheduler refreshScheduler;
    @Inject SyncThrottle syncThrottle;
    @Inject Preferences preferences;
    @Inject Tracker tracker;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences.setupLogger();

        tracker.setTrackingEnabled(preferences.isTrackingEnabled());
    }
}
