package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import org.tasks.injection.ForApplication;
import org.tasks.jobs.JobManager;

import javax.inject.Inject;

public class BackgroundScheduler {
    private final Context context;

    @Inject
    public BackgroundScheduler(@ForApplication Context context) {
        this.context = context;
    }

    public void scheduleEverything() {
        JobIntentService.enqueueWork(context, GeofenceSchedulingIntentService.class, JobManager.JOB_ID_GEOFENCE_SCHEDULING, new Intent());
        JobIntentService.enqueueWork(context, SchedulerIntentService.class, JobManager.JOB_ID_SCHEDULER, new Intent());
        JobIntentService.enqueueWork(context, NotificationSchedulerIntentService.class, JobManager.JOB_ID_NOTIFICATION_SCHEDULER, new Intent());
        scheduleCalendarNotifications();
    }

    public void scheduleCalendarNotifications() {
        JobIntentService.enqueueWork(context, CalendarNotificationIntentService.class, JobManager.JOB_ID_CALENDAR_NOTIFICATION, new Intent());
    }
}
