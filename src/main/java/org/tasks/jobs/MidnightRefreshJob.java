package org.tasks.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;

import org.tasks.Broadcaster;

public class MidnightRefreshJob extends Job {

    public static final String TAG = "job_midnight_refresh";

    private final Broadcaster broadcaster;
    private final JobManager jobManager;

    public MidnightRefreshJob(Broadcaster broadcaster, JobManager jobManager) {
        this.broadcaster = broadcaster;
        this.jobManager = jobManager;
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        try {
            broadcaster.refresh();
            return Result.SUCCESS;
        } finally {
            jobManager.scheduleMidnightRefresh(false);
        }
    }
}
