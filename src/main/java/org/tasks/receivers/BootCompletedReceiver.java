package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

import timber.log.Timber;

public class BootCompletedReceiver extends InjectingBroadcastReceiver {

    @Inject BackgroundScheduler backgroundScheduler;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Timber.d("onReceive(context, %s)", intent);

        backgroundScheduler.scheduleEverything();
    }
}
