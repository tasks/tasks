package org.tasks.jobs;

import android.content.Intent;

import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Notifier;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class ReminderJob extends WakefulJob {

    public static final String TAG = "job_reminder";

    @Inject Preferences preferences;
    @Inject ReminderService reminderService;
    @Inject Notifier notifier;

    public ReminderJob() {
        super(ReminderJob.class.getSimpleName());
    }

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

    @Override
    protected void completeWakefulIntent(Intent intent) {
        ReminderJobBroadcast.completeWakefulIntent(intent);
    }
}
