package org.tasks.scheduling;

import android.content.Intent;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.reminders.ReminderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingIntentService;

import javax.inject.Inject;

public class ReminderSchedulerIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(ReminderSchedulerIntentService.class);

    @Inject ReminderService reminderService;
    @Inject TaskDao taskDao;

    public ReminderSchedulerIntentService() {
        super(ReminderSchedulerIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        log.debug("onHandleIntent({})", intent);

        reminderService.scheduleAllAlarms(taskDao);
    }
}
