package org.tasks.notifications;

import android.app.Notification;
import android.content.Context;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificationManager {

    private final android.app.NotificationManager notificationManager;

    @Inject
    public NotificationManager(@ForApplication Context context) {
        notificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void cancel(long id) {
        notificationManager.cancel((int) id);
    }

    public void notify(int notificationId, Notification notification) {
        notificationManager.notify(notificationId, notification);
    }
}
