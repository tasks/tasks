package org.tasks.scheduling;

import android.content.Intent;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.injection.InjectingIntentService;

import javax.inject.Inject;

import timber.log.Timber;

public class ReminderSchedulerIntentService extends InjectingIntentService {

    @Inject ReminderService reminderService;
    @Inject TaskDao taskDao;

    public ReminderSchedulerIntentService() {
        super(ReminderSchedulerIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        Timber.d("onHandleIntent(%s)", intent);

        reminderService.scheduleAllAlarms(taskDao);
    }
}
