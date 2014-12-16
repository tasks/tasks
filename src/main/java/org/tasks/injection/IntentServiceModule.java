package org.tasks.injection;

import org.tasks.scheduling.*;

import dagger.Module;

@Module(addsTo = TasksModule.class,
        injects = {
                AlarmSchedulingIntentService.class,
                BackupIntentService.class,
                MidnightRefreshService.class,
                RefreshSchedulerIntentService.class,
                ReminderSchedulerIntentService.class
        })
public class IntentServiceModule {
}
