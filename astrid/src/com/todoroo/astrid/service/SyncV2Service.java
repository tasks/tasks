package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

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
    private final SyncV2Provider[] providers = new SyncV2Provider[] {
            new ActFmSyncV2Provider(),
            new GtasksSyncV2Provider()
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

    public boolean hasActiveProvider() {
        for (SyncV2Provider provider : providers) {
            if (provider.isActive()) return true;
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
