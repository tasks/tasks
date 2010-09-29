package com.todoroo.astrid.producteev;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.andlib.utility.Preferences;

/**
 * SynchronizationService is the service that performs Astrid's background
 * synchronization with online task managers. Starting this service
 * schedules a repeating alarm which handles the synchronization
 *
 * @author Tim Su
 *
 */
public class ProducteevBackgroundService extends Service {

	/** Minimum time before an auto-sync */
	private static final long AUTO_SYNC_MIN_OFFSET = 5*60*1000L;

    /** alarm identifier */
    public static final String SYNC_ACTION = "sync"; //$NON-NLS-1$

    // --- BroadcastReceiver abstract methods

    /** Receive the alarm - start the synchronize service! */
    @Override
    public void onStart(Intent intent, int startId) {
        try {
            if(intent != null && SYNC_ACTION.equals(intent.getAction()))
                startSynchronization(this);
        } catch (Exception e) {
            PluginServices.getExceptionService().reportError("pdv-bg-sync", e); //$NON-NLS-1$
        }
    }

    /** Start the actual synchronization */
    private void startSynchronization(Context context) {
        if(context == null || context.getResources() == null)
            return;

        ContextManager.setContext(context);

        if(ProducteevUtilities.INSTANCE.isOngoing())
            return;

        PluginServices.getTaskService();
        FlurryAgent.onStartSession(context, Constants.FLURRY_KEY);
        new ProducteevSyncProvider().synchronize(context);
        FlurryAgent.onEndSession(context);
    }

    // --- alarm management

    /**
     * Schedules repeating alarm for auto-synchronization
     */
    public static void scheduleService() {
        int syncFrequencySeconds = Preferences.getIntegerFromString(
                R.string.producteev_PPr_interval_key, -1);
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
        Intent intent = new Intent(context, ProducteevBackgroundService.class);
        intent.setAction(SYNC_ACTION);
        return intent;
    }

    // --- utility methods


    private static long computeNextSyncOffset(long interval) {
        // figure out last synchronize time
        long lastSyncDate = ProducteevUtilities.INSTANCE.getLastSyncDate();

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
