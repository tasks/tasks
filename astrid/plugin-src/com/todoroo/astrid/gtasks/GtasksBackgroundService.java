/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.sync.SyncV2BackgroundService;
import com.todoroo.astrid.sync.SyncV2Provider;

public class GtasksBackgroundService extends SyncV2BackgroundService {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    @Override
    protected SyncV2Provider getSyncProvider() {
        return GtasksSyncV2Provider.getInstance();
    }

    @Override
    protected SyncProviderUtilities getSyncUtilities() {
        if(gtasksPreferenceService == null)
            DependencyInjectionService.getInstance().inject(this);
        return gtasksPreferenceService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StatisticsService.sessionStart(this);
    }

    @Override
    public void onDestroy() {
        StatisticsService.sessionStop(this);
        super.onDestroy();
    }

}
