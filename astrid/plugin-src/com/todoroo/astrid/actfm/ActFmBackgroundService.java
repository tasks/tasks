package com.todoroo.astrid.actfm;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncProvider;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncBackgroundService;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * SynchronizationService is the service that performs Astrid's background
 * synchronization with online task managers. Starting this service
 * schedules a repeating alarm which handles the synchronization
 *
 * @author Tim Su
 *
 */
public class ActFmBackgroundService extends SyncBackgroundService {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    public ActFmBackgroundService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected SyncProvider<?> getSyncProvider() {
        return new ActFmSyncProvider();
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
