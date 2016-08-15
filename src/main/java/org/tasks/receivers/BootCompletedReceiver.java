package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

import timber.log.Timber;

public class BootCompletedReceiver extends InjectingBroadcastReceiver {

    @Inject BackgroundScheduler backgroundScheduler;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        Timber.d("onReceive(context, %s)", intent);

        backgroundScheduler.scheduleEverything();
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
