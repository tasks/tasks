package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.gtasks.sync.GtasksSyncProvider;
import com.todoroo.astrid.sync.SyncBackgroundService;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.utility.Constants;

public class GtasksBackgroundService extends SyncBackgroundService {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    @Override
    protected SyncProvider<?> getSyncProvider() {
        return new GtasksSyncProvider();
    }

    @Override
    protected SyncProviderUtilities getSyncUtilities() {
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
