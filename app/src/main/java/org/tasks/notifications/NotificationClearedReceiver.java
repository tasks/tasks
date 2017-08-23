package org.tasks.notifications;

import android.content.Context;
import android.content.Intent;

import org.tasks.db.AppDatabase;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

import timber.log.Timber;

public class NotificationClearedReceiver extends InjectingBroadcastReceiver {

    @Inject NotificationManager notificationManager;
    @Inject AppDatabase appDatabase;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        long notificationId = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L);
        Timber.d("cleared %s", notificationId);
        notificationManager.cancel(notificationId);
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
