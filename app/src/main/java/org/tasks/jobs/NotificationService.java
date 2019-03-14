package org.tasks.jobs;

import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import java.util.List;
import javax.inject.Inject;
import org.tasks.Notifier;
import org.tasks.R;
import org.tasks.injection.InjectingService;
import org.tasks.injection.ServiceComponent;
import org.tasks.preferences.Preferences;

public class NotificationService extends InjectingService {

  @Inject Preferences preferences;
  @Inject Notifier notifier;
  @Inject NotificationQueue notificationQueue;

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
  protected synchronized void doWork() {
    assertNotMainThread();

    if (!preferences.isCurrentlyQuietHours()) {
      List<? extends NotificationQueueEntry> overdueJobs = notificationQueue.getOverdueJobs();
      if (!notificationQueue.remove(overdueJobs)) {
        throw new RuntimeException("Failed to remove jobs from queue");
      }
      notifier.triggerNotifications(transform(overdueJobs, NotificationQueueEntry::toNotification));
    }
  }

  @Override
  protected void scheduleNext() {
    notificationQueue.scheduleNext();
  }

  @Override
  protected void inject(ServiceComponent component) {
    component.inject(this);
  }
}
