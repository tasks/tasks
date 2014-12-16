package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;

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
    }

    public void scheduleBackupService() {
        context.startService(new Intent(context, BackupIntentService.class));
    }

    public void scheduleMidnightRefresh() {
        context.startService(new Intent(context, MidnightRefreshService.class));
    }
}
