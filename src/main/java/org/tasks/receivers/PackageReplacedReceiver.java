package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

import timber.log.Timber;

public class PackageReplacedReceiver extends InjectingBroadcastReceiver {

    @Inject BackgroundScheduler backgroundScheduler;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED) && context.getPackageName().equals(intent.getData().getSchemeSpecificPart())) {
            Timber.d("onReceive(context, %s)", intent);
            backgroundScheduler.scheduleEverything();
        } else {
            Timber.d("ignoring %s", intent);
        }
    }
}
