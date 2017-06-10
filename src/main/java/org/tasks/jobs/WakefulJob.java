package org.tasks.jobs;

import android.content.Intent;

public abstract class WakefulJob extends Job {

    public WakefulJob(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        completeWakefulIntent(intent);
    }

    protected abstract void completeWakefulIntent(Intent intent);
}
