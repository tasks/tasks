package org.tasks.jobs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.scheduling.AlarmManager;

import javax.inject.Inject;

import timber.log.Timber;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.nextMidnight;
import static org.tasks.time.DateTimeUtils.printTimestamp;

@ApplicationScope
public class JobManager {

    private Context context;
    private AlarmManager alarmManager;

    @Inject
    public JobManager(@ForApplication Context context, AlarmManager alarmManager) {
        this.context = context;
        this.alarmManager = alarmManager;
    }

    public void schedule(String tag, long time) {
        Timber.d("%s: %s", tag, printTimestamp(time));
        alarmManager.wakeup(adjust(time), getPendingIntent(tag));
    }

    public void scheduleRefresh(long time) {
        Timber.d("%s: %s", RefreshJob.TAG, printTimestamp(time));
        alarmManager.noWakeup(adjust(time), getPendingService(RefreshJob.class));
    }

    public void scheduleMidnightRefresh() {
        long time = nextMidnight();
        Timber.d("%s: %s", MidnightRefreshJob.TAG, printTimestamp(time));
        alarmManager.noWakeup(adjust(time), getPendingService(MidnightRefreshJob.class));
    }

    public void scheduleMidnightBackup() {
        long time = nextMidnight();
        Timber.d("%s: %s", BackupJob.TAG, printTimestamp(time));
        alarmManager.noWakeup(adjust(time), getPendingService(BackupJob.class));
    }

    public void cancel(String tag) {
        Timber.d("CXL %s", tag);
        alarmManager.cancel(getPendingIntent(tag));
    }

    private long adjust(long time) {
        return Math.max(time, currentTimeMillis() + 5000);
    }

    private PendingIntent getPendingIntent(String tag) {
        switch (tag) {
            case ReminderJob.TAG:
                return getPendingBroadcast(ReminderJobBroadcast.class);
            case AlarmJob.TAG:
                return getPendingBroadcast(AlarmJobBroadcast.class);
            case RefreshJob.TAG:
                return getPendingService(RefreshJob.class);
            default:
                throw new RuntimeException("Unexpected tag: " + tag);
        }
    }

    private <T> PendingIntent getPendingBroadcast(Class<T> c) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, c), 0);
    }

    private <T> PendingIntent getPendingService(Class<T> c) {
        return PendingIntent.getService(context, 0, new Intent(context, c), 0);
    }
}
