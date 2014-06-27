package org.tasks.sync;

import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.Broadcaster;

public class RecordSyncStatusCallback implements SyncResultCallback {

    private SyncProviderUtilities syncProviderUtilities;
    private Broadcaster broadcaster;

    public RecordSyncStatusCallback(SyncProviderUtilities syncProviderUtilities) {
        this(syncProviderUtilities, null);
    }

    public RecordSyncStatusCallback(SyncProviderUtilities syncProviderUtilities, Broadcaster broadcaster) {
        this.syncProviderUtilities = syncProviderUtilities;
        this.broadcaster = broadcaster;
    }

    @Override
    public void started() {
        syncProviderUtilities.recordSyncStart();
    }

    @Override
    public void finished() {
        syncProviderUtilities.stopOngoing();
        if (broadcaster != null) {
            broadcaster.eventRefresh();
        }
    }
}
