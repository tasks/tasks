package org.tasks.jobs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import org.tasks.LocalBroadcastManager;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.scheduling.RefreshScheduler;

import javax.inject.Inject;

public class RefreshJob extends Job {

    public static class Broadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JobIntentService.enqueueWork(context, RefreshJob.class, JobManager.JOB_ID_REFRESH, intent);
        }
    }

    public static final String TAG = "job_refresh";

    @Inject RefreshScheduler refreshScheduler;
    @Inject LocalBroadcastManager localBroadcastManager;

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }

    @Override
    protected void run() {
        localBroadcastManager.broadcastRefresh();
    }

    @Override
    protected void scheduleNext() {
        refreshScheduler.scheduleNext();
    }
}
