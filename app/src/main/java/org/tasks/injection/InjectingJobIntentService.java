package org.tasks.injection;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

public abstract class InjectingJobIntentService extends JobIntentService {

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        inject(((InjectingApplication) getApplication())
                .getComponent()
                .plus(new IntentServiceModule()));
    }


    protected abstract void inject(IntentServiceComponent component);
}
