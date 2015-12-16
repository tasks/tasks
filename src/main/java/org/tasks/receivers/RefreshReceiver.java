package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.tasks.Broadcaster;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

import timber.log.Timber;

public class RefreshReceiver extends InjectingBroadcastReceiver {

    @Inject Broadcaster broadcaster;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Timber.d("onReceive(context, %s)", intent);

        broadcaster.refresh();
    }
}
