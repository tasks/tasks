package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;

import org.tasks.Notifier;
import org.tasks.injection.InjectingBroadcastReceiver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class TaskNotificationReceiver extends InjectingBroadcastReceiver {

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static final String ID_KEY = "id"; //$NON-NLS-1$
    public static final String EXTRAS_TYPE = "type"; //$NON-NLS-1$

    @Inject Notifier notifier;

    @Override
    public void onReceive(Context context, final Intent intent) {
        super.onReceive(context, intent);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                notifier.triggerTaskNotification(
                        intent.getLongExtra(ID_KEY, 0),
                        intent.getIntExtra(EXTRAS_TYPE, (byte) 0));
            }
        });
    }
}
