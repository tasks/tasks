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

    /**
     * Initialize list of injectables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     */
    @Override
    protected void addInjectables() {
        new Injector().inject(this);

        // com.todoroo.astrid.dao
        injectables.put("database", database);
        injectables.put("taskDao", TaskDao.class);
        injectables.put("metadataDao", MetadataDao.class);
        injectables.put("tagMetadataDao", TagMetadataDao.class);
        injectables.put("tagDataDao", TagDataDao.class);
        injectables.put("storeObjectDao", StoreObjectDao.class);
        injectables.put("userActivityDao", UserActivityDao.class);
        injectables.put("taskAttachmentDao", TaskAttachmentDao.class);
        injectables.put("taskListMetadataDao", TaskListMetadataDao.class);

        // com.todoroo.astrid.service
        injectables.put("taskService", TaskService.class);
        injectables.put("metadataService", MetadataService.class);
        injectables.put("tagDataService", TagDataService.class);
        injectables.put("upgradeService", UpgradeService.class);
        injectables.put("syncService", SyncV2Service.class);

        // com.todoroo.astrid.gtasks
        injectables.put("gtasksPreferenceService", GtasksPreferenceService.class);
        injectables.put("gtasksListService", GtasksListService.class);
        injectables.put("gtasksMetadataService", GtasksMetadataService.class);
        injectables.put("gtasksTaskListUpdater", GtasksTaskListUpdater.class);
        injectables.put("gtasksSyncService", GtasksSyncService.class);

        // com.todoroo.astrid.tags
        injectables.put("tagService", TagService.class);

        injectables.put("broadcaster", Broadcaster.class);

        injectables.put("filterCounter", FilterCounter.class);
        injectables.put("refreshScheduler", RefreshScheduler.class);
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
