package org.tasks.jobs;

import android.content.Intent;

import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.Notifier;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class AlarmJob extends WakefulJob {

    public static final String TAG = "job_alarm";

    @Inject Preferences preferences;
    @Inject AlarmService alarmService;
    @Inject Notifier notifier;
    @Inject TaskDao taskDao;

    public AlarmJob() {
        super(AlarmJob.class.getSimpleName());
    }

    @Override
    protected void run() {
        if (!preferences.isCurrentlyQuietHours()) {
            for (Alarm alarm : alarmService.getOverdueAlarms()) {
                Task task = taskDao.fetch(alarm.getTaskId(), Task.REMINDER_LAST);
                if (task != null && task.getReminderLast() < alarm.getTime()) {
                    notifier.triggerTaskNotification(alarm.getTaskId(), ReminderService.TYPE_ALARM);
                }
                alarmService.remove(alarm);
            }
        }
    }

    @Override
    protected void scheduleNext() {
        alarmService.scheduleNextJob();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }

    @Override
    protected void completeWakefulIntent(Intent intent) {
        AlarmJobBroadcast.completeWakefulIntent(intent);
    }
}
