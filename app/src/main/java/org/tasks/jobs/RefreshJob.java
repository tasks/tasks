package org.tasks.jobs;

import org.tasks.Broadcaster;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.scheduling.RefreshScheduler;

import javax.inject.Inject;

public class RefreshJob extends Job {

    public static final String TAG = "job_refresh";

    @Inject RefreshScheduler refreshScheduler;
    @Inject Broadcaster broadcaster;

    public RefreshJob() {
        super(RefreshJob.class.getSimpleName());
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }

    @Override
    protected void run() {
        broadcaster.refresh();
    }

    @Override
    protected void scheduleNext() {
        refreshScheduler.scheduleNext();
    }
}
