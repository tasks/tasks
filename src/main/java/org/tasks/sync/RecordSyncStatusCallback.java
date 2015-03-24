package org.tasks.sync;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.Broadcaster;

public class RecordSyncStatusCallback implements SyncResultCallback {

    private GtasksPreferenceService gtasksPreferenceService;
    private Broadcaster broadcaster;

    public RecordSyncStatusCallback(GtasksPreferenceService gtasksPreferenceService) {
        this(gtasksPreferenceService, null);
    }

    public RecordSyncStatusCallback(GtasksPreferenceService gtasksPreferenceService, Broadcaster broadcaster) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.broadcaster = broadcaster;
    }

    @Override
    public void started() {
        gtasksPreferenceService.recordSyncStart();
    }

    @Override
    public void finished() {
        gtasksPreferenceService.stopOngoing();
        if (broadcaster != null) {
            broadcaster.refresh();
        }
    }
}
