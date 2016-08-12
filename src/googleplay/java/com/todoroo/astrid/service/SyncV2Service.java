/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.sync.SyncExecutor;

import javax.inject.Inject;

/**
 * SyncV2Service is a simplified synchronization interface for supporting
 * next-generation sync interfaces such as Google Tasks and Astrid.com
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class SyncV2Service {
    /*
     * At present, sync provider interactions are handled through code. If
     * there is enough interest, the Astrid team could create an interface
     * for responding to sync requests through this new API.
     */
    private final SyncExecutor syncExecutor;
    private final SyncAdapterHelper syncAdapterHelper;
    private final GtasksSyncService gtasksSyncService;

    @Inject
    public SyncV2Service(SyncExecutor syncExecutor, SyncAdapterHelper syncAdapterHelper, GtasksSyncService gtasksSyncService) {
        this.syncExecutor = syncExecutor;
        this.syncAdapterHelper = syncAdapterHelper;
        this.gtasksSyncService = gtasksSyncService;
    }

    public void clearCompleted(final GtasksList list, final SyncResultCallback callback) {
        if (syncAdapterHelper.isEnabled()) {
            syncExecutor.execute(callback, () -> {
                callback.started();
                try {
                    gtasksSyncService.clearCompleted(list.getRemoteId());
                } finally {
                    callback.finished();
                }
            });
        }
    }
}
