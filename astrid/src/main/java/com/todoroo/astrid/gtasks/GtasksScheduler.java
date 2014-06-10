package com.todoroo.astrid.gtasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.utility.DateUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GtasksScheduler {

    private static final Logger log = LoggerFactory.getLogger(GtasksScheduler.class);

    /** Minimum time before an auto-sync */
    private static final long AUTO_SYNC_MIN_OFFSET = 5*60*1000L;

    private final GtasksPreferenceService gtasksPreferenceService;
    private Context context;
    private Preferences preferences;

    @Inject
    public GtasksScheduler(GtasksPreferenceService gtasksPreferenceService, @ForApplication Context context, Preferences preferences) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.context = context;
        this.preferences = preferences;
    }

    /**
     * Schedules repeating alarm for auto-synchronization
     */
    public void scheduleService() {
        int syncFrequencySeconds = 0;
        try {
            syncFrequencySeconds = preferences.getIntegerFromString(
                    gtasksPreferenceService.getSyncIntervalKey(), -1);
        } catch(ClassCastException e) {
            log.error(e.getMessage(), e);
            preferences.setStringFromInteger(gtasksPreferenceService.getSyncIntervalKey(), 0);
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

        log.info("Autosync set for {} seconds repeating every {}", offset / 1000, syncFrequencySeconds); //$NON-NLS-1$

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
