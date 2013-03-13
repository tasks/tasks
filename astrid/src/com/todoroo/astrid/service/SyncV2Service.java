/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.service.SyncResultCallbackWrapper.WidgetUpdatingCallbackWrapper;
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
    private static final SyncV2Provider[] providers = new SyncV2Provider[] {
            GtasksSyncV2Provider.getInstance(),
            new ActFmSyncV2Provider(),
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
     * @return true if any servide was logged in and initiated a sync
     */
    public boolean synchronizeActiveTasks(final boolean manual, SyncResultCallback callback) {
        final List<SyncV2Provider> active = activeProviders();

        if (active.size() == 0)
            return false;

        if (active.size() > 1) { // This should never happen anymore--they can't be active at the same time, but if for some reason they both are, just use ActFm
            active.get(1).synchronizeActiveTasks(manual, new WidgetUpdatingCallbackWrapper(callback));
        } else if (active.size() == 1) {
            active.get(0).synchronizeActiveTasks(manual, new WidgetUpdatingCallbackWrapper(callback));
        }

        return true;
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
                provider.synchronizeList(list, manual, new WidgetUpdatingCallbackWrapper(callback));
        }
    }

}
