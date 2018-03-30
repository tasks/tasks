package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "alarms")
public class Alarm {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient long id;

  @ColumnInfo(name = "task")
  private transient long task;

  @ColumnInfo(name = "time")
  private long time;

  public Alarm() {}

  @Ignore
  public Alarm(long task, long time) {
    this.task = task;
    this.time = time;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getTask() {
    return task;
  }

  public void setTask(long task) {
    this.task = task;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Alarm alarm = (Alarm) o;

    if (id != alarm.id) {
      return false;
    }
    if (task != alarm.task) {
      return false;
    }
    return time == alarm.time;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (task ^ (task >>> 32));
    result = 31 * result + (int) (time ^ (time >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Alarm{" + "id=" + id + ", task=" + task + ", time=" + time + '}';
  }
}
