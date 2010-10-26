package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncProvider;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncBackgroundService;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;

public class GtasksBackgroundService extends SyncBackgroundService {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    @Override
    protected SyncProvider<?> getSyncProvider() {
        return new GtasksSyncProvider();
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
