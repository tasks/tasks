package org.tasks.jobs;

import org.tasks.notifications.Notification;

public interface JobQueueEntry {
    long getId();

    long getTime();

    Notification toNotification();
}
