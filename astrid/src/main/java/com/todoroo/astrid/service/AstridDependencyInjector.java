/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.service.AbstractDependencyInjector;
import com.todoroo.andlib.service.DependencyInjectionService;
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
import com.todoroo.astrid.tags.TagService;

import org.tasks.Broadcaster;
import org.tasks.Injector;
import org.tasks.filters.FilterCounter;
import org.tasks.scheduling.RefreshScheduler;

import javax.inject.Inject;

/**
 * Astrid application dependency injector loads classes in Astrid with the
 * appropriate instantiated objects necessary for their operation. For
 * more information on Dependency Injection, see {@link DependencyInjectionService}
 * and {@link AbstractDependencyInjector}.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AstridDependencyInjector extends AbstractDependencyInjector {

    /**
     * Boolean bit to prevent multiple copies of this injector to be loaded
     */
    private static AstridDependencyInjector instance = null;

    @Inject Database database;
    @Inject MetadataDao metadataDao;
    @Inject TagDataDao tagDataDao;
    @Inject Broadcaster broadcaster;
    @Inject TaskDao taskDao;
    @Inject TagMetadataDao tagMetadataDao;
    @Inject StoreObjectDao storeObjectDao;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject TagDataService tagDataService;
    @Inject MetadataService metadataService;
    @Inject SyncV2Service syncV2Service;
    @Inject FilterCounter filterCounter;
    @Inject RefreshScheduler refreshScheduler;
    @Inject TaskService taskService;
    @Inject TagService tagService;

    /**
     * Initialize list of injectables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     */
    @Override
    protected void addInjectables() {
        new Injector().inject(this);

        // com.todoroo.astrid.dao
        injectables.put("database", database);
        injectables.put("taskDao", taskDao);
        injectables.put("metadataDao", metadataDao);
        injectables.put("tagMetadataDao", tagMetadataDao);
        injectables.put("tagDataDao", tagDataDao);
        injectables.put("storeObjectDao", storeObjectDao);
        injectables.put("userActivityDao", userActivityDao);
        injectables.put("taskAttachmentDao", taskAttachmentDao);
        injectables.put("taskListMetadataDao", taskListMetadataDao);

        // com.todoroo.astrid.service
        injectables.put("taskService", taskService);
        injectables.put("metadataService", metadataService);
        injectables.put("tagDataService", tagDataService);
        injectables.put("upgradeService", UpgradeService.class);
        injectables.put("syncService", syncV2Service);

        // com.todoroo.astrid.gtasks
        injectables.put("gtasksPreferenceService", GtasksPreferenceService.class);
        injectables.put("gtasksListService", GtasksListService.class);
        injectables.put("gtasksMetadataService", GtasksMetadataService.class);
        injectables.put("gtasksTaskListUpdater", GtasksTaskListUpdater.class);
        injectables.put("gtasksSyncService", GtasksSyncService.class);

        // com.todoroo.astrid.tags
        injectables.put("tagService", tagService);

        injectables.put("broadcaster", broadcaster);

        injectables.put("filterCounter", filterCounter);
        injectables.put("refreshScheduler", refreshScheduler);
    }

    /**
     * Install this service as the default Dependency Injector
     */
    public static void initialize() {
        if(instance != null) {
            return;
        }
        synchronized(AstridDependencyInjector.class) {
            if(instance == null) {
                instance = new AstridDependencyInjector();
            }
            DependencyInjectionService.getInstance().addInjector(instance);
        }
    }

    public static void inject(Object caller) {
        initialize();
        DependencyInjectionService.getInstance().inject(caller);
    }

    AstridDependencyInjector() {
        // prevent instantiation
        super();
    }

    /**
     * Flush dependency injection cache. Useful for unit tests.
     */
    public synchronized static void reset() {
        instance = null;
        initialize();
    }
}
