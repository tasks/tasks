package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Broadcaster;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

public class RefreshReceiver extends InjectingBroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(RefreshReceiver.class);

    @Inject Broadcaster broadcaster;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        log.debug("onReceive(context, {})", intent);

        broadcaster.refresh();
    }
}
