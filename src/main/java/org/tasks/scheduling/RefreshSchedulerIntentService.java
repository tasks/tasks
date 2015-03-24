package org.tasks.scheduling;

import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingIntentService;

import javax.inject.Inject;

public class RefreshSchedulerIntentService extends InjectingIntentService {

    private static final Logger log = LoggerFactory.getLogger(RefreshSchedulerIntentService.class);

    @Inject RefreshScheduler refreshScheduler;

    public RefreshSchedulerIntentService() {
        super(RefreshSchedulerIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        log.debug("onHandleIntent({})", intent);

        refreshScheduler.scheduleApplicationRefreshes();
    }
}
