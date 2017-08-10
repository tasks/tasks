package org.tasks.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.Context;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastO;

public class NotificationManager {

    private final android.app.NotificationManager notificationManager;
    private final Preferences preferences;
    public static final String DEFAULT_NOTIFICATION_CHANNEL = "my_channel_01";

    @Inject
    public NotificationManager(@ForApplication Context context, Preferences preferences) {
        this.preferences = preferences;
        notificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (atLeastO()) {
            String channelName = context.getString(R.string.notifications);
            NotificationChannel notificationChannel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL, channelName, android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(preferences.isLEDNotificationEnabled());
            notificationChannel.enableVibration(preferences.isVibrationEnabled());
            notificationChannel.setLightColor(preferences.getLEDColor());
            notificationChannel.setVibrationPattern(preferences.getVibrationPattern());
            notificationManager.createNotificationChannel(notificationChannel);
        }
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
