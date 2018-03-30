package org.tasks.jobs;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import org.tasks.notifications.Notification;

public class ReminderEntry implements NotificationQueueEntry {

  private final long taskId;
  private final long time;
  private final int type;

  public ReminderEntry(long taskId, long time, int type) {
    this.taskId = taskId;
    this.time = time;
    this.type = type;
  }

  @Override
  public long getId() {
    return taskId;
  }

  @Override
  public long getTime() {
    return time;
  }

  @Override
  public Notification toNotification() {
    Notification notification = new Notification();
    notification.taskId = taskId;
    notification.type = type;
    notification.timestamp = currentTimeMillis();
    return notification;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ReminderEntry reminderEntry = (ReminderEntry) o;

    if (taskId != reminderEntry.taskId) {
      return false;
    }
    if (time != reminderEntry.time) {
      return false;
    }
    return type == reminderEntry.type;
  }

  @Override
  public int hashCode() {
    int result = (int) (taskId ^ (taskId >>> 32));
    result = 31 * result + (int) (time ^ (time >>> 32));
    result = 31 * result + type;
    return result;
  }

  @Override
  public String toString() {
    return "ReminderEntry{" + "taskId=" + taskId + ", time=" + time + ", type=" + type + '}';
  }
}
