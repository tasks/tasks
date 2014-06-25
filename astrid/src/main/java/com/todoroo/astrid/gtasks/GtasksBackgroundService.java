/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingService;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

public class GtasksBackgroundService extends InjectingService {

    private static final Logger log = LoggerFactory.getLogger(GtasksBackgroundService.class);

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksSyncV2Provider gtasksSyncV2Provider;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /** Receive the alarm - start the synchronize service! */
    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if(intent != null && !started.getAndSet(true)) {
                startSynchronization(this);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /** Start the actual synchronization */
    private void startSynchronization(final Context context) {
        if(context == null || context.getResources() == null) {
            return;
        }

        ContextManager.setContext(context);

        if(!gtasksPreferenceService.isLoggedIn()) {
            return;
        }

        SyncV2Provider provider = gtasksSyncV2Provider;
        if (provider.isActive()) {
            provider.synchronizeActiveTasks(new SyncResultCallback() {
                @Override
                public void started() {
                }

                @Override
                public void finished() {
                    gtasksPreferenceService.recordSuccessfulSync();
                    context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
