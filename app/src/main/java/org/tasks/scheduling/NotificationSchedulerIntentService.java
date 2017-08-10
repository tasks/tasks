package org.tasks.scheduling;

import android.content.Intent;

import com.todoroo.astrid.alarms.AlarmService;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.IntentServiceComponent;

import javax.inject.Inject;

import timber.log.Timber;

public class NotificationSchedulerIntentService extends InjectingJobIntentService {

    @Inject AlarmService alarmService;
    @Inject ReminderService reminderService;
    @Inject TaskDao taskDao;

    @Override
    protected void onHandleWork(Intent intent) {
        super.onHandleWork(intent);

        Timber.d("onHandleIntent(%s)", intent);

        reminderService.clear();
        alarmService.clear();

        reminderService.scheduleAllAlarms(taskDao);
        alarmService.scheduleAllAlarms();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
