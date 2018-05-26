package org.tasks.jobs;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import com.todoroo.astrid.reminders.ReminderService;
import org.tasks.notifications.Notification;

public class AlarmEntry implements NotificationQueueEntry {

  private final long alarmId;
  private final long taskId;
  private final long time;

  public AlarmEntry(org.tasks.data.Alarm alarm) {
    this(alarm.getId(), alarm.getTask(), alarm.getTime());
  }

  public AlarmEntry(long alarmId, long taskId, Long time) {
    this.alarmId = alarmId;
    this.taskId = taskId;
    this.time = time;
  }

  @Override
  public long getId() {
    return alarmId;
  }

  @Override
  public long getTime() {
    return time;
  }

  @Override
  public Notification toNotification() {
    Notification notification = new Notification();
    notification.taskId = taskId;
    notification.type = ReminderService.TYPE_ALARM;
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

    AlarmEntry alarmEntry = (AlarmEntry) o;

    if (alarmId != alarmEntry.alarmId) {
      return false;
    }
    if (taskId != alarmEntry.taskId) {
      return false;
    }
    return time == alarmEntry.time;
  }

  @Override
  public int hashCode() {
    int result = (int) (alarmId ^ (alarmId >>> 32));
    result = 31 * result + (int) (taskId ^ (taskId >>> 32));
    result = 31 * result + (int) (time ^ (time >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "AlarmEntry{" + "alarmId=" + alarmId + ", taskId=" + taskId + ", time=" + time + '}';
  }
}
