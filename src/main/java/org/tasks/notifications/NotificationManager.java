package org.tasks.notifications;

import android.app.Notification;
import android.content.Context;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificationManager {

    private final android.app.NotificationManager notificationManager;
    private final Preferences preferences;

    @Inject
    public NotificationManager(@ForApplication Context context, Preferences preferences) {
        this.preferences = preferences;
        notificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void cancel(long id) {
        notificationManager.cancel((int) id);
    }

    public void notify(int notificationId, Notification notification) {
        if (preferences.getBoolean(R.string.p_rmd_enabled, true)) {
            notificationManager.notify(notificationId, notification);
        }
    }
}
