package org.tasks.notifications;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

@Entity(
    tableName = "notification",
    indices = {@Index(value = "task", unique = true)})
public class Notification {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "uid")
  public int uid;

  @ColumnInfo(name = "task")
  public long taskId;

  @ColumnInfo(name = "timestamp")
  public long timestamp;

  @ColumnInfo(name = "type")
  public int type;

  @Override
  public String toString() {
    return "Notification{"
        + "uid="
        + uid
        + ", taskId="
        + taskId
        + ", timestamp="
        + timestamp
        + ", type="
        + type
        + '}';
  }
}
