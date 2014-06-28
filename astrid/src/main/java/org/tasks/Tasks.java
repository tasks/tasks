package org.tasks;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.filters.FilterCounter;
import org.tasks.injection.InjectingApplication;
import org.tasks.scheduling.RefreshScheduler;

import javax.inject.Inject;

@SuppressWarnings("UnusedDeclaration")
public class Tasks extends InjectingApplication {

    @Inject Database database;
    @Inject TaskDao taskDao;
    @Inject MetadataDao metadataDao;
    @Inject TagMetadataDao tagMetadataDao;
    @Inject TagDataDao tagDataDao;
    @Inject StoreObjectDao storeObjectDao;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject TaskService taskService;
    @Inject MetadataService metadataService;
    @Inject TagDataService tagDataService;
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

    @Override
    public void onCreate() {
        super.onCreate();

        ContextManager.setContext(this);
    }
}
