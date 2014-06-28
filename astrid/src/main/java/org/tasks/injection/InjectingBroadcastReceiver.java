package org.tasks.injection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InjectingBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ((Injector) context.getApplicationContext())
                .getObjectGraph()
                .plus(new BroadcastModule())
                .inject(this);
    }
}
