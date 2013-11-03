/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;

/**
 * Utility class for getting dependency-injected services from plugins
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class PluginServices {

    @Autowired
    TaskService taskService;

    @Autowired
    Database database;

    @Autowired
    ExceptionService exceptionService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    TagDataService tagDataService;

    @Autowired
    TagDataDao tagDataDao;

    @Autowired
    StoreObjectDao storeObjectDao;

    @Autowired
    TaskListMetadataDao taskListMetadataDao;

    private static volatile PluginServices instance;

    static {
        AstridDependencyInjector.initialize();
    }

    private PluginServices() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private static PluginServices getInstance() {
        if(instance == null) {
            synchronized (PluginServices.class) {
                if (instance == null) {
                    instance = new PluginServices();
                }
            }
        }
        return instance;
    }

    public static TaskService getTaskService() {
        getInstance().database.openForWriting();
        return getInstance().taskService;
    }

    public static TagDataService getTagDataService() {
        return getInstance().tagDataService;
    }

    public static TagDataDao getTagDataDao() {
        return getInstance().tagDataDao;
    }

    public static ExceptionService getExceptionService() {
        return getInstance().exceptionService;
    }

    public static MetadataService getMetadataService() {
        getInstance().database.openForWriting();
        return getInstance().metadataService;
    }

    public static StoreObjectDao getStoreObjectDao() {
        return getInstance().storeObjectDao;
    }

    public static TaskListMetadataDao getTaskListMetadataDao() {
        return getInstance().taskListMetadataDao;
    }
}
