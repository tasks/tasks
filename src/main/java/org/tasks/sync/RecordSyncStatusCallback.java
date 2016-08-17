package org.tasks.sync;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.Broadcaster;

public class RecordSyncStatusCallback implements SyncResultCallback {

    private final GtasksPreferenceService gtasksPreferenceService;
    private final Broadcaster broadcaster;

    public RecordSyncStatusCallback(GtasksPreferenceService gtasksPreferenceService, Broadcaster broadcaster) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.broadcaster = broadcaster;
    }

    @Override
    public void started() {
        gtasksPreferenceService.recordSyncStart();
        broadcaster.refresh();
    }

    @Override
    public void finished() {
        gtasksPreferenceService.stopOngoing();
        broadcaster.refresh();
    }
}
