package org.tasks.jobs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;

import org.tasks.BuildConfig;
import org.tasks.Notifier;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.preferences.Preferences;

import java.util.List;

import javax.inject.Inject;

public class NotificationJob extends Job {

    public static class Broadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JobIntentService.enqueueWork(context, NotificationJob.class, JobManager.JOB_ID_NOTIFICATION, intent);
        }
    }

    public static final String TAG = "job_notification";

    @Inject Preferences preferences;
    @Inject Notifier notifier;
    @Inject JobQueue jobQueue;

    @Override
    protected void run() {
        if (!preferences.isCurrentlyQuietHours()) {
            List<? extends JobQueueEntry> overdueJobs = jobQueue.getOverdueJobs();
            notifier.triggerTaskNotifications(overdueJobs);
            boolean success = jobQueue.remove(overdueJobs);
            if (BuildConfig.DEBUG && !success) {
                throw new RuntimeException("Failed to remove jobs from queue");
            }
        }
    }

    @Override
    protected void scheduleNext() {
        jobQueue.scheduleNext();
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
