package org.tasks.caldav;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import timber.log.Timber;

public class CalDAVSyncService extends Service {
    private static final Object lock = new Object();
    private static CalDAVSyncAdapter syncAdapter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("Service created");
        synchronized (lock) {
            if (syncAdapter == null) {
                syncAdapter = new CalDAVSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
