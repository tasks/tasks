/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;

/**
 * Performs synchronization service logic in background service to avoid
 * ANR (application not responding) messages.
 * <p>
 * Starting this service
 *  schedules a repeating alarm which handles
 * synchronization with your serv
 *
 * @author Tim Su
 *
 */
abstract public class SyncV2BackgroundService extends Service {

	/** Minimum time before an auto-sync */
	private static final long AUTO_SYNC_MIN_OFFSET = 5*60*1000L;

    @Autowired private ExceptionService exceptionService;

    // --- abstract methods

    abstract protected SyncV2Provider getSyncProvider();

    abstract protected SyncProviderUtilities getSyncUtilities();

    // --- implementation

    public SyncV2BackgroundService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private final AtomicBoolean started = new AtomicBoolean(false);

    /** Receive the alarm - start the synchronize service! */
    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if(intent != null && !started.getAndSet(true)) {
                startSynchronization(this);
            }
        } catch (Exception e) {
            exceptionService.reportError(getSyncUtilities().getIdentifier() + "-bg-sync", e); //$NON-NLS-1$
        }
    }

    /** Start the actual synchronization */
    private void startSynchronization(final Context context) {
        if(context == null || context.getResources() == null)
            return;

        ContextManager.setContext(context);

        if(!getSyncUtilities().isLoggedIn())
            return;

        SyncV2Provider provider = getSyncProvider();
        if (provider.isActive())
            provider.synchronizeActiveTasks(false, new SyncResultCallbackAdapter() {
                @Override
                public void finished() {
                    getSyncUtilities().recordSuccessfulSync();
                    context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                }
            });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public synchronized void stop() {
        started.set(false);
        stopSelf();
    }

    // --- alarm management

    /**
     * Schedules repeating alarm for auto-synchronization
     */
    public void scheduleService() {
        int syncFrequencySeconds = 0;
        try {
            syncFrequencySeconds = Preferences.getIntegerFromString(
                    getSyncUtilities().getSyncIntervalKey(), -1);
        } catch(ClassCastException e) {
            Preferences.setStringFromInteger(getSyncUtilities().getSyncIntervalKey(), 0);
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
        PendingIntent pendingIntent = PendingIntent.getService(context, getSyncUtilities().getSyncIntervalKey(),
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
        PendingIntent pendingIntent = PendingIntent.getService(context, getSyncUtilities().getSyncIntervalKey(),
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
    }

    /** Create the alarm intent */
    private Intent createAlarmIntent(Context context) {
        Intent intent = new Intent(context, getClass());
        return intent;
    }

    // --- utility methods

    private long computeNextSyncOffset(long interval) {
        // figure out last synchronize time
        long lastSyncDate = getSyncUtilities().getLastSyncDate();

        // if user never synchronized, give them a full offset period before bg sync
        if(lastSyncDate != 0)
            return Math.max(0, lastSyncDate + interval - DateUtilities.now());
        else
            return interval;
    }


}
