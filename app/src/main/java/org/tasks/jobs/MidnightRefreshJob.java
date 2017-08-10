package org.tasks.jobs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import org.tasks.LocalBroadcastManager;
import org.tasks.injection.IntentServiceComponent;

import javax.inject.Inject;

public class MidnightRefreshJob extends MidnightJob {

    public static class Broadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JobIntentService.enqueueWork(context, MidnightRefreshJob.class, JobManager.JOB_ID_MIDNIGHT_REFRESH, intent);
        }
    }

    public static final String TAG = "job_midnight_refresh";

    @Inject LocalBroadcastManager localBroadcastManager;
    @Inject JobManager jobManager;

    @Override
    protected void run() {
        localBroadcastManager.broadcastRefresh();
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
