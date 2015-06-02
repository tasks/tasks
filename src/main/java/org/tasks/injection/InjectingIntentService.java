package org.tasks.injection;

import android.app.IntentService;
import android.content.Intent;

public abstract class InjectingIntentService extends IntentService {

    public InjectingIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ((Injector) getApplication())
                .getObjectGraph()
                .plus(new IntentServiceModule())
                .inject(this);
    }
}
