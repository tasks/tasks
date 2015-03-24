package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

public class PackageReplacedReceiver extends InjectingBroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(PackageReplacedReceiver.class);

    @Inject BackgroundScheduler backgroundScheduler;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED) && context.getPackageName().equals(intent.getData().getSchemeSpecificPart())) {
            log.debug("onReceive(context, {})", intent);
            backgroundScheduler.scheduleEverything();
        } else {
            log.debug("ignoring {}", intent);
        }
    }
}
