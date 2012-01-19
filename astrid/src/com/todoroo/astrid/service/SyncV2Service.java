package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    abstract public static class SyncV2Provider {
        /**
         * @return sync provider name (displayed in sync menu)
         */
        abstract public String getName();

        /**
         * @return true if this provider is logged in
         */
        abstract public boolean isActive();

        /**
         * Synchronize all of user's active tasks
         * @param manual whether manually triggered
         * @param callback callback object
         */
        abstract public void synchronizeActiveTasks(boolean manual, SyncResultCallback callback);

        /**
         * Synchronize a single list
         * @param list object representing list (TaskListActivity-dependent)
         * @param manual whether was manually triggered
         * @param callback callback object
         */
        abstract public void synchronizeList(Object list, boolean manual, SyncResultCallback callback);

        @Override
        public String toString() {
            return getName();
        }
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
     * Returns active sync providers
     *
     * @param callback
     */
    public List<SyncV2Provider> activeProviders() {
        ArrayList<SyncV2Provider> actives = new ArrayList<SyncV2Provider>();
        for(SyncV2Provider provider : providers) {
            if(provider.isActive())
                actives.add(provider);
        }
        return Collections.unmodifiableList(actives);
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
