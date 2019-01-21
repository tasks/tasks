package org.tasks.jobs;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.Notifier;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.injection.InjectingService;
import org.tasks.injection.ServiceComponent;
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
  protected int getNotificationBody() {
    return R.string.building_notifications;
  }

  @Override
  protected void doWork() {
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
