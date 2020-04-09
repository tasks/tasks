package org.tasks.jobs;

import org.tasks.notifications.Notification;

interface NotificationQueueEntry {

  long getId();

  long getTime();

  Notification toNotification();
}
