/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Context;

import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.injection.ForApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SyncV2Service is a simplified synchronization interface for supporting
 * next-generation sync interfaces such as Google Tasks and Astrid.com
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
@Singleton
public class SyncV2Service {

    /*
     * At present, sync provider interactions are handled through code. If
     * there is enough interest, the Astrid team could create an interface
     * for responding to sync requests through this new API.
     */
    private final GtasksSyncV2Provider[] providers;
    private final Context context;

    @Inject
    public SyncV2Service(@ForApplication Context context, GtasksSyncV2Provider gtasksSyncV2Provider) {
        this.context = context;
        providers = new GtasksSyncV2Provider[] {
                gtasksSyncV2Provider
        };
    }

    /**
     * Returns active sync providers
     */
    public List<GtasksSyncV2Provider> activeProviders() {
        ArrayList<GtasksSyncV2Provider> actives = new ArrayList<>();
        for(GtasksSyncV2Provider provider : providers) {
            if(provider.isActive()) {
                actives.add(provider);
            }
        }
        return Collections.unmodifiableList(actives);
    }

    /**
     * Initiate synchronization of active tasks
     *
     * @param callback result callback
     * @return true if any servide was logged in and initiated a sync
     */
    public boolean synchronizeActiveTasks(SyncResultCallback callback) {
        final List<GtasksSyncV2Provider> active = activeProviders();

        if (active.size() == 0) {
            return false;
        }

        if (active.size() > 1) { // This should never happen anymore--they can't be active at the same time, but if for some reason they both are, just use ActFm
            active.get(1).synchronizeActiveTasks(new WidgetUpdatingCallbackWrapper(context, callback));
        } else if (active.size() == 1) {
            active.get(0).synchronizeActiveTasks(new WidgetUpdatingCallbackWrapper(context, callback));
        }

        return true;
    }

    /**
     * Initiate synchronization of task list
     *
     * @param list list object
     * @param callback result callback
     */
    public void synchronizeList(Object list, SyncResultCallback callback) {
        for(GtasksSyncV2Provider provider : providers) {
            if(provider.isActive()) {
                provider.synchronizeList(list, new WidgetUpdatingCallbackWrapper(context, callback));
            }
        }
    }
}
