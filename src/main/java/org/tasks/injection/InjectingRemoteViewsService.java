package org.tasks.injection;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.RemoteViewsService;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
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
