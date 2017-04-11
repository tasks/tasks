package org.tasks.jobs;

import android.content.Context;

import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobRequest;

import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.time.DateTimeUtils.nextMidnight;

@ApplicationScope
public class JobManager {

    private final com.evernote.android.job.JobManager jobManager;

    @Inject
    public JobManager(@ForApplication Context context) {
        jobManager = com.evernote.android.job.JobManager.create(context);
        jobManager.cancelAll();
    }

    public void addJobCreator(JobCreator jobCreator) {
        jobManager.addJobCreator(jobCreator);
    }

    public void schedule(String tag, long time, boolean cancelCurrent) {
        new JobRequest.Builder(tag)
                .setExact(Math.max(time - currentTimeMillis(), 5000))
                .setUpdateCurrent(cancelCurrent)
                .build()
                .schedule();
    }

    public void scheduleRefresh(long time, boolean cancelExisting) {
        new JobRequest.Builder(RefreshJob.TAG)
                .setExact(Math.max(time - currentTimeMillis(), 5000))
                .setUpdateCurrent(cancelExisting)
                .build()
                .schedule();
    }

    public void scheduleMidnightRefresh(boolean cancelExisting) {
        scheduleMidnightJob(MidnightRefreshJob.TAG, cancelExisting);
    }

    public void scheduleMidnightBackup(boolean cancelExisting) {
        scheduleMidnightJob(BackupJob.TAG, cancelExisting);
    }

    private void scheduleMidnightJob(String tag, boolean cancelExisting) {
        long now = System.currentTimeMillis();
        new JobRequest.Builder(tag)
                .setExact(nextMidnight(now) - now)
                .setUpdateCurrent(cancelExisting)
                .build()
                .schedule();
    }

    public void cancel(String tag) {
        jobManager.cancelAllForTag(tag);
    }

    public void cancelRefreshes() {
        jobManager.cancelAllForTag(RefreshJob.TAG);
    }
}
