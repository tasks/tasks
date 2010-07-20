package com.todoroo.astrid.core;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.service.TaskService;

public final class PluginServices {

    @Autowired
    TaskService taskService;

    private static PluginServices instance;

    private PluginServices() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private synchronized static PluginServices getInstance() {
        if(instance == null)
            instance = new PluginServices();
        return instance;
    }

    public static TaskService getTaskService() {
        return getInstance().taskService;
    }

}
