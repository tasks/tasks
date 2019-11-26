package org.tasks.notifications;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Table;

@Entity(
    tableName = Notification.TABLE_NAME,
    indices = {@Index(value = "task", unique = true)})
public class Notification {

  public static final String TABLE_NAME = "notification";

  public static final Table TABLE = new Table(TABLE_NAME);
  public static final LongProperty TASK = new LongProperty(TABLE, "task");

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
