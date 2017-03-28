package org.tasks.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;

import org.tasks.Broadcaster;
import org.tasks.scheduling.RefreshScheduler;

public class RefreshJob extends Job {

    public static final String TAG = "job_refresh";

    private final RefreshScheduler refreshScheduler;
    private final Broadcaster broadcaster;

    public RefreshJob(RefreshScheduler refreshScheduler, Broadcaster broadcaster) {
        this.refreshScheduler = refreshScheduler;
        this.broadcaster = broadcaster;
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        try {
            broadcaster.refresh();
            return Result.SUCCESS;
        } finally {
            refreshScheduler.scheduleNext();
        }
    }
}
