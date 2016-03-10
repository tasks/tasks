package org.tasks.scheduling;

import org.tasks.Broadcaster;
import org.tasks.injection.IntentServiceComponent;

import javax.inject.Inject;

public class RefreshSchedulerIntentService extends MidnightIntentService {

    @Inject Broadcaster broadcaster;
    @Inject RefreshScheduler refreshScheduler;

    public RefreshSchedulerIntentService() {
        super(RefreshSchedulerIntentService.class.getSimpleName());
    }

    @Override
    void run() {
        refreshScheduler.scheduleApplicationRefreshes();
        broadcaster.refresh();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
