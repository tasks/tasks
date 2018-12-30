package org.tasks.location;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;

public class GeofenceTransitionsIntentService extends Service {

    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class Broadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

}
