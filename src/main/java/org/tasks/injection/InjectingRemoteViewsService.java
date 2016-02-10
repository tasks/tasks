package org.tasks.injection;

import android.widget.RemoteViewsService;

public abstract class InjectingRemoteViewsService extends RemoteViewsService {
    @Override
    public void onCreate() {
        super.onCreate();

        ((Injector) getApplication())
                .getObjectGraph()
                .plus(new ServiceModule())
                .inject(this);
    }
}
