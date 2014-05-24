/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;
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

    /** Minimum time before an auto-sync */
    private static final long AUTO_SYNC_MIN_OFFSET = 5*60*1000L;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /** Receive the alarm - start the synchronize service! */
    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if(intent != null && !started.getAndSet(true)) {
                startSynchronization(this);
            }
        } catch (Exception e) {
            log.error("{}-bg-sync", gtasksPreferenceService.getIdentifier(), e);
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
            provider.synchronizeActiveTasks(false, new SyncResultCallbackAdapter() {
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

    // --- alarm management

    /**
     * Schedules repeating alarm for auto-synchronization
     */
    public void scheduleService() {
        int syncFrequencySeconds = 0;
        try {
            syncFrequencySeconds = Preferences.getIntegerFromString(
                    gtasksPreferenceService.getSyncIntervalKey(), -1);
        } catch(ClassCastException e) {
            Preferences.setStringFromInteger(gtasksPreferenceService.getSyncIntervalKey(), 0);
        }
        Context context = ContextManager.getContext();
        if(syncFrequencySeconds <= 0) {
            unscheduleService(context);
            return;
        }

        // figure out synchronization frequency
        long interval = 1000L * syncFrequencySeconds;
        long offset = computeNextSyncOffset(interval);

        // give a little padding
        offset = Math.max(offset, AUTO_SYNC_MIN_OFFSET);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, gtasksPreferenceService.getSyncIntervalKey(),
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);

        Log.i("Astrid", "Autosync set for " + offset / 1000 //$NON-NLS-1$ //$NON-NLS-2$
                + " seconds repeating every " + syncFrequencySeconds); //$NON-NLS-1$

        // cancel all existing
        am.cancel(pendingIntent);

        // schedule new
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + offset,
                interval, pendingIntent);
    }


    /**
     * Removes repeating alarm for auto-synchronization
     */
    private void unscheduleService(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, gtasksPreferenceService.getSyncIntervalKey(),
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
    }

    /** Create the alarm intent */
    private Intent createAlarmIntent(Context context) {
        return new Intent(context, getClass());
    }

    // --- utility methods

    private long computeNextSyncOffset(long interval) {
        // figure out last synchronize time
        long lastSyncDate = gtasksPreferenceService.getLastSyncDate();

        // if user never synchronized, give them a full offset period before bg sync
        if(lastSyncDate != 0) {
            return Math.max(0, lastSyncDate + interval - DateUtilities.now());
        } else {
            return interval;
        }
    }

}
