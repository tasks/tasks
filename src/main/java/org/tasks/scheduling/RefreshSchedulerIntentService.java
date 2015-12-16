package org.tasks.scheduling;

import android.content.Intent;

import org.tasks.injection.InjectingIntentService;

import javax.inject.Inject;

import timber.log.Timber;

public class RefreshSchedulerIntentService extends InjectingIntentService {

    @Inject RefreshScheduler refreshScheduler;

    public RefreshSchedulerIntentService() {
        super(RefreshSchedulerIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        Timber.d("onHandleIntent(%s)", intent);

        refreshScheduler.scheduleApplicationRefreshes();
    }
}
