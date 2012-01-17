package com.todoroo.astrid.service;

import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;

/**
 * SyncV2Service is a simplified synchronization interface for supporting
 * next-generation sync interfaces such as Google Tasks and Astrid.com
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class SyncV2Service {

    public interface SyncResultCallback {
        /**
         * Increment max sync progress
         * @param incrementBy
         */
        public void incrementMax(int incrementBy);

        /**
         * Increment current sync progress
         * @param incrementBy
         */
        public void incrementProgress(int incrementBy);

        /**
         * Provider started sync
         */
        public void started();

        /**
         * Provider finished sync
         */
        public void finished();
    }

    public interface SyncV2Provider {
        public boolean isActive();
        public void synchronizeActiveTasks(boolean manual, SyncResultCallback callback);
        public void synchronizeList(Object list, boolean manual, SyncResultCallback callback);
    }

    /*
     * At present, sync provider interactions are handled through code. If
     * there is enough interest, the Astrid team could create an interface
     * for responding to sync requests through this new API.
     */
    private final SyncV2Provider[] providers = new SyncV2Provider[] {
            new ActFmSyncV2Provider()
    };

    /**
     * Determine if synchronization is available
     *
     * @param callback
     */
    public boolean isActive() {
        for(SyncV2Provider provider : providers) {
            if(provider.isActive())
                return true;
        }
        return false;
    }

    /**
     * Initiate synchronization of active tasks
     *
     * @param manual if manual sync
     * @param callback result callback
     */
    public void synchronizeActiveTasks(boolean manual, SyncResultCallback callback) {
        for(SyncV2Provider provider : providers) {
            if(provider.isActive())
                provider.synchronizeActiveTasks(manual, callback);
        }
    }

    /**
     * Initiate synchronization of task list
     *
     * @param list list object
     * @param manual if manual sync
     * @param callback result callback
     */
    public void synchronizeList(Object list, boolean manual, SyncResultCallback callback) {
        for(SyncV2Provider provider : providers) {
            if(provider.isActive())
                provider.synchronizeList(list, manual, callback);
        }
    }

}
