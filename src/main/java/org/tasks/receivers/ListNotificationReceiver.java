package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.tasks.Notifier;
import org.tasks.injection.InjectingBroadcastReceiver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import timber.log.Timber;

public class ListNotificationReceiver extends InjectingBroadcastReceiver {

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static final String EXTRA_FILTER_TITLE = "extra_filter_title";
    public static final String EXTRA_FILTER_QUERY = "extra_filter_query";
    public static final String EXTRA_FILTER_VALUES = "extra_filter_values";

    @Inject Notifier notifier;

    @Override
    public void onReceive(Context context, final Intent intent) {
        super.onReceive(context, intent);

        Timber.i("onReceive(%s, %s)", context, intent);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                notifier.triggerFilterNotification(
                        intent.getStringExtra(EXTRA_FILTER_TITLE),
                        intent.getStringExtra(EXTRA_FILTER_QUERY),
                        intent.getStringExtra(EXTRA_FILTER_VALUES));
            }
        });
    }
}
