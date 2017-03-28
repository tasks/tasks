package org.tasks.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Notifier;
import org.tasks.preferences.Preferences;

public class AlarmJob extends Job {

    public static final String TAG = "job_alarm";

    private final Preferences preferences;
    private final AlarmService alarmService;
    private final Notifier notifier;
    private final TaskDao taskDao;

    public AlarmJob(Preferences preferences, AlarmService alarmService, Notifier notifier, TaskDao taskDao) {
        this.preferences = preferences;
        this.alarmService = alarmService;
        this.notifier = notifier;
        this.taskDao = taskDao;
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        try {
            if (!preferences.isCurrentlyQuietHours()) {
                for (Alarm alarm : alarmService.removePastDueAlarms()) {
                    Task task = taskDao.fetch(alarm.getTaskId(), Task.REMINDER_LAST);
                    if (task != null && task.getReminderLast() < alarm.getTime()) {
                        notifier.triggerTaskNotification(alarm.getTaskId(), ReminderService.TYPE_ALARM);
                    }
                }
            }
            return Result.SUCCESS;
        } finally {
            alarmService.scheduleNextJob();
        }
    }
}
