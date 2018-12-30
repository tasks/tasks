package org.tasks.notifications;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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

  @ColumnInfo(name = "location")
  public Long location;

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
        + ", location="
        + location
        + '}';
  }
}
