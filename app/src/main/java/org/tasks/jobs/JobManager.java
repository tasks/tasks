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

    static final int JOB_ID_REFRESH = 1;
    public static final int JOB_ID_BACKGROUND_SCHEDULER = 2;
    static final int JOB_ID_NOTIFICATION = 3;
    public static final int JOB_ID_GEOFENCE_TRANSITION = 4;
    public static final int JOB_ID_GEOFENCE_SCHEDULING = 5;
    static final int JOB_ID_MIDNIGHT_REFRESH = 6;
    static final int JOB_ID_BACKUP = 7;
    public static final int JOB_ID_TASK_STATUS_CHANGE = 8;
    public static final int JOB_ID_NOTIFICATION_SCHEDULER = 9;
    public static final int JOB_ID_CALENDAR_NOTIFICATION = 10;

    private Context context;
    private AlarmManager alarmManager;

    @Inject
    public JobManager(@ForApplication Context context, AlarmManager alarmManager) {
        this.context = context;
        this.alarmManager = alarmManager;
    }

    @SuppressWarnings("WeakerAccess")
    public void schedule(String tag, long time) {
        Timber.d("%s: %s", tag, printTimestamp(time));
        alarmManager.wakeup(adjust(time), getPendingIntent(tag));
    }

    public void scheduleRefresh(long time) {
        Timber.d("%s: %s", RefreshJob.TAG, printTimestamp(time));
        alarmManager.noWakeup(adjust(time), getPendingBroadcast(RefreshJob.Broadcast.class));
    }

    public void scheduleMidnightRefresh() {
        long time = nextMidnight();
        Timber.d("%s: %s", MidnightRefreshJob.TAG, printTimestamp(time));
        alarmManager.noWakeup(adjust(time), getPendingBroadcast(MidnightRefreshJob.Broadcast.class));
    }

    public void scheduleMidnightBackup() {
        long time = nextMidnight();
        Timber.d("%s: %s", BackupJob.TAG, printTimestamp(time));
        alarmManager.wakeup(adjust(time), getPendingBroadcast(BackupJob.Broadcast.class));
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
            case NotificationJob.TAG:
                return getPendingBroadcast(NotificationJob.Broadcast.class);
            case RefreshJob.TAG:
                return getPendingBroadcast(RefreshJob.Broadcast.class);
            default:
                throw new RuntimeException("Unexpected tag: " + tag);
        }
    }

    private <T> PendingIntent getPendingBroadcast(Class<T> c) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, c), 0);
    }
}
