package org.tasks.sync;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.LocalBroadcastManager;

public class RecordSyncStatusCallback implements SyncResultCallback {

    private final GtasksPreferenceService gtasksPreferenceService;
    private final LocalBroadcastManager localBroadcastManager;

    public RecordSyncStatusCallback(GtasksPreferenceService gtasksPreferenceService,
                                    LocalBroadcastManager localBroadcastManager) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.localBroadcastManager = localBroadcastManager;
    }

    @Override
    public void started() {
        gtasksPreferenceService.recordSyncStart();
        localBroadcastManager.broadcastRefresh();
    }

    @Override
    public void finished() {
        gtasksPreferenceService.stopOngoing();
        localBroadcastManager.broadcastRefresh();
    }
}
