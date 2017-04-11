package org.tasks.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Notifier;
import org.tasks.preferences.Preferences;

public class ReminderJob extends Job {

    public static final String TAG = "job_reminder";

    private final Preferences preferences;
    private final ReminderService reminderService;
    private final Notifier notifier;

    public ReminderJob(Preferences preferences, ReminderService reminderService, Notifier notifier) {
        this.preferences = preferences;
        this.reminderService = reminderService;
        this.notifier = notifier;
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        try {
            if (!preferences.isCurrentlyQuietHours()) {
                for (Reminder reminder : reminderService.getPastReminders()) {
                    notifier.triggerTaskNotification(reminder.getId(), reminder.getType());
                }
            }
            return Result.SUCCESS;
        } finally {
            reminderService.scheduleNextJob();
        }
    }
}
