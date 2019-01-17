package org.tasks.jobs;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.Notifier;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.injection.InjectingService;
import org.tasks.injection.ServiceComponent;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.Preferences;

public class NotificationService extends InjectingService {

  @Inject Preferences preferences;
  @Inject Notifier notifier;
  @Inject NotificationQueue notificationQueue;
  @Inject Tracker tracker;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  protected int getNotificationId() {
    return -1;
  }

  @Override
  protected Notification getNotification() {
    return new NotificationCompat.Builder(
            this, NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS)
        .setSound(null)
        .setSmallIcon(R.drawable.ic_check_white_24dp)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.building_notifications))
        .build();
  }

  @Override
  protected void doWork(@Nonnull Intent intent) {
    try {
      if (!preferences.isCurrentlyQuietHours()) {
        List<? extends NotificationQueueEntry> overdueJobs = notificationQueue.getOverdueJobs();
        notifier.triggerTaskNotifications(overdueJobs);
        boolean success = notificationQueue.remove(overdueJobs);
        if (BuildConfig.DEBUG && !success) {
          throw new RuntimeException("Failed to remove jobs from queue");
        }
      }
    } catch (Exception e) {
      tracker.reportException(e);
    } finally {
      notificationQueue.scheduleNext();
    }
  }

  @Override
  protected void inject(ServiceComponent component) {
    component.inject(this);
  }
}
