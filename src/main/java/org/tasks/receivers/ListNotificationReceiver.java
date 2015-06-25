package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.Notifier;
import org.tasks.injection.InjectingBroadcastReceiver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class ListNotificationReceiver extends InjectingBroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(ListNotificationReceiver.class);

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static final String EXTRA_FILTER_TITLE = "extra_filter_title";
    public static final String EXTRA_FILTER_QUERY = "extra_filter_query";

    @Inject Notifier notifier;

    @Override
    public void onReceive(Context context, final Intent intent) {
        super.onReceive(context, intent);

        log.info("onReceive({}, {}", context, intent);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                notifier.triggerFilterNotification(
                        intent.getStringExtra(EXTRA_FILTER_TITLE),
                        intent.getStringExtra(EXTRA_FILTER_QUERY));
            }
        });
    }
}
