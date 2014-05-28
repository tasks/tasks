/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Context;

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
import com.todoroo.astrid.gtasks.GtasksScheduler;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.Broadcaster;
import org.tasks.filters.FilterCounter;
import org.tasks.injection.TestInjector;
import org.tasks.scheduling.RefreshScheduler;
import org.tasks.widget.WidgetHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.tasks.injection.TasksModule.ForApplication;

/**
 * Astrid application dependency injector loads classes in Astrid with the
 * appropriate instantiated objects necessary for their operation. For
 * more information on Dependency Injection, see {@link com.todoroo.andlib.service.DependencyInjectionService}
 * and {@link com.todoroo.andlib.service.AbstractDependencyInjector}.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AstridDependencyInjector extends AbstractDependencyInjector {

    @Module(
            injects = {
                    AstridDependencyInjector.class
            }
    )
    public static class TestModule {
        private Context context;

        public TestModule(Context context) {
            this.context = context;
        }

        @Singleton
        @Provides
        @ForApplication
        public Context getContext() {
            return context;
        }
    }

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
    @Inject UpgradeService upgradeService;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject GtasksSyncService gtasksSyncService;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksSyncV2Provider gtasksSyncV2Provider;
    @Inject WidgetHelper widgetHelper;
    @Inject GtasksScheduler gtasksScheduler;
    @Inject ReminderService reminderService;

    /**
     * Initialize list of injectables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     */
    @Override
    protected void addInjectables(Context context) {
        new TestInjector(context)
                .inject(this, new TestModule(context));

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
        injectables.put("upgradeService", upgradeService);
        injectables.put("syncService", syncV2Service);

        // com.todoroo.astrid.gtasks
        injectables.put("gtasksPreferenceService", gtasksPreferenceService);
        injectables.put("gtasksListService", gtasksListService);
        injectables.put("gtasksMetadataService", gtasksMetadataService);
        injectables.put("gtasksTaskListUpdater", gtasksTaskListUpdater);
        injectables.put("gtasksSyncService", gtasksSyncService);
        injectables.put("gtasksSyncV2Provider", gtasksSyncV2Provider);

        // com.todoroo.astrid.tags
        injectables.put("tagService", tagService);

        injectables.put("broadcaster", broadcaster);

        injectables.put("filterCounter", filterCounter);
        injectables.put("refreshScheduler", refreshScheduler);
        injectables.put("widgetHelper", widgetHelper);
        injectables.put("gtasksScheduler", gtasksScheduler);
        injectables.put("reminderService", reminderService);
    }

    /**
     * Install this service as the default Dependency Injector
     */
    private static void initialize(Context context) {
        if(instance != null) {
            return;
        }
        synchronized(AstridDependencyInjector.class) {
            if(instance == null) {
                instance = new AstridDependencyInjector(context);
            }
            DependencyInjectionService.getInstance().addInjector(instance);
        }
    }

    AstridDependencyInjector(Context context) {
        // prevent instantiation
        super(context);
    }

    /**
     * Flush dependency injection cache. Useful for unit tests.
     */
    public synchronized static void reset(Context context) {
        instance = null;
        initialize(context);
    }
}
