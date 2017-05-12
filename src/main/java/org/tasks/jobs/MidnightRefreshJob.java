package org.tasks.jobs;

import org.tasks.Broadcaster;
import org.tasks.injection.IntentServiceComponent;

import javax.inject.Inject;

public class MidnightRefreshJob extends MidnightJob {

    public static final String TAG = "job_midnight_refresh";

    @Inject Broadcaster broadcaster;
    @Inject JobManager jobManager;

    public MidnightRefreshJob() {
        super(MidnightRefreshJob.class.getSimpleName());
    }

    @Override
    protected void run() {
        broadcaster.refresh();
    }

    @Override
    protected void scheduleNext() {
        jobManager.scheduleMidnightRefresh();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
