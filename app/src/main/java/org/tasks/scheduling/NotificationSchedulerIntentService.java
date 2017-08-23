package org.tasks.scheduling;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Notifier;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.jobs.JobManager;
import org.tasks.jobs.JobQueue;

import javax.inject.Inject;

import timber.log.Timber;

public class NotificationSchedulerIntentService extends InjectingJobIntentService {

    public static void enqueueWork(Context context) {
        JobIntentService.enqueueWork(context, NotificationSchedulerIntentService.class, JobManager.JOB_ID_NOTIFICATION_SCHEDULER, new Intent());
    }

    @Inject AlarmService alarmService;
    @Inject ReminderService reminderService;
    @Inject TaskDao taskDao;
    @Inject JobQueue jobQueue;
    @Inject Notifier notifier;

    @Override
    protected void onHandleWork(Intent intent) {
        super.onHandleWork(intent);

        Timber.d("onHandleWork(%s)", intent);

        jobQueue.clear();

        notifier.restoreNotifications();
        reminderService.scheduleAllAlarms(taskDao);
        alarmService.scheduleAllAlarms();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
