package org.tasks.jobs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Notifier;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class NotificationJob extends Job {

    public static class Broadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JobIntentService.enqueueWork(context, NotificationJob.class, JobManager.JOB_ID_NOTIFICATION, intent);
        }
    }

    public static final String TAG = "job_notification";

    @Inject Preferences preferences;
    @Inject Notifier notifier;
    @Inject TaskDao taskDao;
    @Inject JobQueue jobQueue;

    @Override
    protected void run() {
        if (!preferences.isCurrentlyQuietHours()) {
            for (JobQueueEntry entry : jobQueue.getOverdueJobs()) {
                if (entry instanceof Alarm) {
                    Alarm alarm = (Alarm) entry;
                    Task task = taskDao.fetch(alarm.getTaskId(), Task.REMINDER_LAST);
                    if (task != null && task.getReminderLast() < alarm.getTime()) {
                        notifier.triggerTaskNotification(alarm.getTaskId(), ReminderService.TYPE_ALARM);
                    }
                } else if (entry instanceof Reminder) {
                    Reminder reminder = (Reminder) entry;
                    notifier.triggerTaskNotification(reminder.getId(), reminder.getType());
                }
                jobQueue.remove(entry);
            }
        }
    }

    @Override
    protected void scheduleNext() {
        jobQueue.scheduleNext();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
