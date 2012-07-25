/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk;

import org.weloveastrid.rmilk.sync.MilkSyncProvider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;

/**
 * SynchronizationService is the service that performs Astrid's background
 * synchronization with online task managers. Starting this service
 * schedules a repeating alarm which handles the synchronization
 *
 * @author Tim Su
 *
 */
public class MilkBackgroundService extends Service {

	/** Minimum time before an auto-sync */
	private static final long AUTO_SYNC_MIN_OFFSET = 5*60*1000L;

    /** alarm identifier */
    public static final String SYNC_ACTION = "sync"; //$NON-NLS-1$

    // --- BroadcastReceiver abstract methods

    /** start the synchronization service. sits in the background */
    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if(intent != null && SYNC_ACTION.equals(intent.getAction()))
                startSynchronization(this);
        } catch (Exception e) {
            MilkUtilities.INSTANCE.setLastError(e.toString(), "");
        }
    }

    /** Start the actual synchronization */
    private void startSynchronization(Context context) {
        if(context == null || context.getResources() == null)
            return;

        ContextManager.setContext(context);

        if(MilkUtilities.INSTANCE.isOngoing())
            return;

        new MilkSyncProvider().synchronize(context);
    }

    // --- alarm management

    /**
     * Schedules repeating alarm for auto-synchronization
     */
    public static void scheduleService() {
        Context context = ContextManager.getContext();
        int syncFrequencySeconds = MilkUtilities.INSTANCE.getSyncAutoSyncFrequency();
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
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
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
    private static void unscheduleService(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
    }

    /** Create the alarm intent */
    private static Intent createAlarmIntent(Context context) {
        Intent intent = new Intent(context, MilkBackgroundService.class);
        intent.setAction(SYNC_ACTION);
        return intent;
    }

    // --- utility methods


    private static long computeNextSyncOffset(long interval) {
        // figure out last synchronize time
        long lastSyncDate = MilkUtilities.INSTANCE.getLastSyncDate();

        // if user never synchronized, give them a full offset period before bg sync
        if(lastSyncDate != 0)
            return Math.max(0, lastSyncDate + interval - DateUtilities.now());
        else
            return interval;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
