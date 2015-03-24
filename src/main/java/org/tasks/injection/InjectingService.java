package org.tasks.injection;

import android.app.Service;

public abstract class InjectingService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();

        ((Injector) getApplication())
                .getObjectGraph()
                .plus(new ServiceModule())
                .inject(this);
    }
}
