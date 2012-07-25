/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.sync.SyncV2BackgroundService;
import com.todoroo.astrid.sync.SyncV2Provider;

/**
 * SynchronizationService is the service that performs Astrid's background
 * synchronization with online task managers. Starting this service
 * schedules a repeating alarm which handles the synchronization
 *
 * @author Tim Su
 *
 */
public class ActFmBackgroundService extends SyncV2BackgroundService {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    public ActFmBackgroundService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected SyncV2Provider getSyncProvider() {
        return new ActFmSyncV2Provider();
    }

    @Override
    protected SyncProviderUtilities getSyncUtilities() {
        return actFmPreferenceService;
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
