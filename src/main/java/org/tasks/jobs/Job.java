package org.tasks.jobs;

import android.content.Intent;

import org.tasks.analytics.Tracker;
import org.tasks.injection.InjectingIntentService;

import javax.inject.Inject;

import timber.log.Timber;

public abstract class Job extends InjectingIntentService {

    @Inject Tracker tracker;

    public Job(String name) {
        super(name);
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        Timber.d("onHandleIntent(%s)", intent);

        try {
            run();
        } catch (Exception e) {
            tracker.reportException(e);
        } finally {
            scheduleNext();
        }
    }

    protected abstract void run();

    protected abstract void scheduleNext();
}
