package org.tasks.jobs;

import static org.tasks.time.DateTimeUtils2.currentTimeMillis;
import static org.tasks.time.DateTimeUtilsKt.printTimestamp;

import org.tasks.data.entity.Notification;

import java.util.Objects;

public class AlarmEntry {

  private final long alarmId;
  private final long taskId;
  private final long time;
  private final int type;

  public AlarmEntry(long alarmId, long taskId, Long time, int type) {
    this.alarmId = alarmId;
    this.taskId = taskId;
    this.time = time;
    this.type = type;
  }

  public long getId() {
    return alarmId;
  }

  public long getTime() {
    return time;
  }

  public long getTaskId() {
    return taskId;
  }

  public int getType() {
    return type;
  }

  public Notification toNotification() {
    Notification notification = new Notification();
    notification.setTaskId(taskId);
    notification.setType(type);
    notification.setTimestamp(currentTimeMillis());
    return notification;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AlarmEntry that = (AlarmEntry) o;
    return alarmId == that.alarmId && taskId == that.taskId && time == that.time && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(alarmId, taskId, time, type);
  }

  @Override
  public String toString() {
    return "AlarmEntry{alarmId=" + alarmId + ", taskId=" + taskId + ", time=" + printTimestamp(time) + ", type=" + type + '}';
  }
}
