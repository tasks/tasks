package org.tasks.jobs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Notifier;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class ReminderJob extends Job {

    public static class Broadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JobIntentService.enqueueWork(context, ReminderJob.class, JobManager.JOB_ID_REMINDER, intent);
        }
    }

    public static final String TAG = "job_reminder";

    @Inject Preferences preferences;
    @Inject ReminderService reminderService;
    @Inject Notifier notifier;

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }

    @Override
    protected void run() {
        if (!preferences.isCurrentlyQuietHours()) {
            for (Reminder reminder : reminderService.getPastReminders()) {
                notifier.triggerTaskNotification(reminder.getId(), reminder.getType());
                reminderService.remove(reminder);
            }
        }
    }

    @Override
    protected void scheduleNext() {
        reminderService.scheduleNextJob();
    }
}
