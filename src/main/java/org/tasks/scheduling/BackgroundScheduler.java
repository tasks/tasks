package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class BackgroundScheduler {
    private final Context context;

    @Inject
    public BackgroundScheduler(@ForApplication Context context) {
        this.context = context;
    }

    public void scheduleEverything() {
        context.startService(new Intent(context, GeofenceSchedulingIntentService.class));
        context.startService(new Intent(context, SchedulerIntentService.class));
        context.startService(new Intent(context, NotificationSchedulerIntentService.class));
        scheduleCalendarNotifications();
    }

    public void scheduleCalendarNotifications() {
        context.startService(new Intent(context, CalendarNotificationIntentService.class));
    }
}
