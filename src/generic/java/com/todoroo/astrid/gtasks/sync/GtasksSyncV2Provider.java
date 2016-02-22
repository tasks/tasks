package com.todoroo.astrid.gtasks.sync;

import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.sync.SyncResultCallback;

import javax.inject.Inject;

public class GtasksSyncV2Provider {

    @Inject
    public GtasksSyncV2Provider() {

    }

    public boolean isActive() {
        return false;
    }

    public void synchronizeActiveTasks(SyncResultCallback callback) {

    }

    public void synchronizeList(GtasksList list, SyncResultCallback callback) {

    }

    public void clearCompleted(GtasksList list, SyncResultCallback callback) {

    }
}
