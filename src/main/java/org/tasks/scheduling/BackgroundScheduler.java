package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class BackgroundScheduler {
    private Context context;

    @Inject
    public BackgroundScheduler(@ForApplication Context context) {
        this.context = context;
    }

    public void scheduleEverything() {
        context.startService(new Intent(context, RefreshSchedulerIntentService.class));
        context.startService(new Intent(context, AlarmSchedulingIntentService.class));
        context.startService(new Intent(context, ReminderSchedulerIntentService.class));
        scheduleBackupService();
        scheduleMidnightRefresh();
        scheduleCalendarNotifications();
        if (context.getResources().getBoolean(R.bool.sync_enabled)) {
            scheduleGtaskSync();
        }
    }

    public void scheduleBackupService() {
        context.startService(new Intent(context, BackupIntentService.class));
    }

    public void scheduleMidnightRefresh() {
        context.startService(new Intent(context, MidnightRefreshService.class));
    }

    public void scheduleGtaskSync() {
        context.startService(new Intent(context, GtasksBackgroundService.class));
    }

    public void scheduleCalendarNotifications() {
        context.startService(new Intent(context, CalendarNotificationIntentService.class));
    }
}
