/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Intent;
import android.os.IBinder;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.injection.InjectingService;
import org.tasks.sync.RecordSyncStatusCallback;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

public class GtasksBackgroundService extends InjectingService {

    private static final Logger log = LoggerFactory.getLogger(GtasksBackgroundService.class);

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksSyncV2Provider gtasksSyncV2Provider;
    @Inject Broadcaster broadcaster;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /** Receive the alarm - start the synchronize service! */
    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if(intent != null && !started.getAndSet(true)) {
                ContextManager.setContext(this);

                if(gtasksPreferenceService.isLoggedIn() && gtasksSyncV2Provider.isActive()) {
                    gtasksSyncV2Provider.synchronizeActiveTasks(new RecordSyncStatusCallback(gtasksPreferenceService, broadcaster));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
