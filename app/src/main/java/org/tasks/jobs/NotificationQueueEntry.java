package org.tasks.jobs;

import org.tasks.notifications.Notification;

public interface NotificationQueueEntry {

  long getId();

  long getTime();

  Notification toNotification();
}
