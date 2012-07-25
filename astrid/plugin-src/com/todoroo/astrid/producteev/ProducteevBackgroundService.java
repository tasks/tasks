/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;
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
public class ProducteevBackgroundService extends SyncBackgroundService {

    @Override
    protected SyncProvider<?> getSyncProvider() {
        return new ProducteevSyncProvider();
    }

    @Override
    protected SyncProviderUtilities getSyncUtilities() {
        return ProducteevUtilities.INSTANCE;
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
