package com.todoroo.astrid.gtasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GtasksScheduler {

    /** Minimum time before an auto-sync */
    private static final long AUTO_SYNC_MIN_OFFSET = 5*60*1000L;

    private final GtasksPreferenceService gtasksPreferenceService;
    private Context context;

    @Inject
    public GtasksScheduler(GtasksPreferenceService gtasksPreferenceService, @ForApplication Context context) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.context = context;
    }

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
        return new Intent(context, GtasksBackgroundService.class);
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
