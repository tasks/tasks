package org.tasks.injection;

import android.app.IntentService;
import android.content.Intent;

public abstract class InjectingIntentService extends IntentService {

    protected InjectingIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        inject(((InjectingApplication) getApplication())
                .getComponent()
                .plus(new IntentServiceModule()));
    }

    protected abstract void inject(IntentServiceComponent component);
}
