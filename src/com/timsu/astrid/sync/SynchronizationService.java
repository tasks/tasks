package com.timsu.astrid.sync;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.Preferences;

/**
 * SynchronizationService is the service that performs Astrid's background
 * synchronization with online task managers. Starting this service
 * schedules a repeating alarm which handles the synchronization
 *
 * @author Tim Su
 *
 */
public class SynchronizationService extends BroadcastReceiver {

	/** miniumum time before an auto-sync */
	private static final long AUTO_SYNC_MIN_OFFSET = 5*60*1000L;

    /** alarm identifier */
    private static final String SYNC_ACTION = "sync";

    // --- BroadcastReceiver abstract methods

    /** Receive the alarm - start the synchronize service! */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(SYNC_ACTION))
            startSynchronization(context);
    }

    /** Start the actual synchronization */
    private void startSynchronization(Context context) {
        if(context == null || context.getResources() == null)
            return;

        // figure out synchronization frequency
        Integer syncFrequencySeconds = Preferences.getSyncAutoSyncFrequency(context);
        if(syncFrequencySeconds == null)
            return;
        long interval = 1000L * syncFrequencySeconds;
        long offset = computeNextSyncOffset(context, interval);

        // if premature request for sync (i.e. user invoked manual sync,
        // reschedule our service
        if(offset != 0) {
            Log.i("astrid", "Automatic Synchronize Postponed.");
            return;
        }

        Log.i("astrid", "Automatic Synchronize Initiated.");
        Preferences.setSyncLastSyncAttempt(context, new Date());

        Synchronizer sync = new Synchronizer(true);
        sync.synchronize(context, null);
    }

    // --- alarm management

    /**
     * Schedules repeating alarm for auto-synchronization
     */
    public static void scheduleService(Context context) {
        Integer syncFrequencySeconds = Preferences.getSyncAutoSyncFrequency(context);

        if(syncFrequencySeconds == null) {
    	    unscheduleService(context);
    	    return;
    	}

    	// figure out synchronization frequency
        long interval = 1000L * syncFrequencySeconds;
        long offset = computeNextSyncOffset(context, interval);

        // give a little padding
        offset = Math.max(offset, AUTO_SYNC_MIN_OFFSET);

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);

        if (Constants.DEBUG)
            Log.e("Astrid", "Autosync set for " + offset / 1000
                + " seconds repeating every " + syncFrequencySeconds);

        // cancel all existing
        am.cancel(pendingIntent);

        // schedule new
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + offset,
                interval, pendingIntent);
    }


    /**
     * Removes repeating alarm for auto-synchronization
     */
    public static void unscheduleService(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
    }

    /** Create the alarm intent */
    private static Intent createAlarmIntent(Context context) {
        Intent intent = new Intent(context, SynchronizationService.class);
        intent.setAction(SYNC_ACTION);
        return intent;
    }

    // --- utility methods


    private static long computeNextSyncOffset(Context context, long interval) {
        // figure out last synchronize time
        Date lastSyncDate = Preferences.getSyncLastSync(context);
        Date lastAutoSyncDate = Preferences.getSyncLastSyncAttempt(context);

        // if user never synchronized, give them a full offset period before bg sync
        long latestSyncMillis = System.currentTimeMillis();
        if(lastSyncDate != null)
            latestSyncMillis = lastSyncDate.getTime();
        if(lastAutoSyncDate != null && lastAutoSyncDate.getTime() > latestSyncMillis)
            latestSyncMillis = lastAutoSyncDate.getTime();
        long offset = 0;
        if(latestSyncMillis != 0)
            offset = Math.max(0, latestSyncMillis + interval - System.currentTimeMillis());

        return offset;
    }



}
