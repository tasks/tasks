package org.tasks.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import java.util.Objects;

@Entity(
    tableName = "google_tasks",
    indices = {
      @Index(name = "gt_task", value = "gt_task"),
      @Index(
          name = "gt_list_parent",
          value = {"gt_list_id", "gt_parent"})
    })
public class GoogleTask {

  public static final String KEY = "gtasks"; // $NON-NLS-1$

  public static final Table TABLE = new Table("google_tasks");

  public static final Property.IntegerProperty PARENT =
      new Property.IntegerProperty(TABLE, "gt_parent");

  public static final Property.IntegerProperty TASK =
      new Property.IntegerProperty(TABLE, "gt_task");

  public static final Property.LongProperty DELETED =
      new Property.LongProperty(TABLE, "gt_deleted");

  public static final Property.StringProperty LIST =
      new Property.StringProperty(TABLE, "gt_list_id");

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "gt_id")
  private transient long id;

  @ColumnInfo(name = "gt_task")
  private transient long task;

  @ColumnInfo(name = "gt_remote_id")
  private String remoteId = "";

  @ColumnInfo(name = "gt_list_id")
  private String listId = "";

  @ColumnInfo(name = "gt_parent")
  private transient long parent;

  @ColumnInfo(name = "gt_remote_parent")
  private String remoteParent;

  @ColumnInfo(name = "gt_moved")
  private transient boolean moved;

  @ColumnInfo(name = "gt_order")
  private transient long order;

  @ColumnInfo(name = "gt_remote_order")
  private long remoteOrder;

  @ColumnInfo(name = "gt_last_sync")
  private long lastSync;

  @ColumnInfo(name = "gt_deleted")
  private long deleted;

  public GoogleTask() {}

  @Ignore
  public GoogleTask(long task, String listId) {
    this.task = task;
    this.listId = listId;
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

  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public String getListId() {
    return listId;
  }

  public void setListId(String listId) {
    this.listId = listId;
  }

  public long getParent() {
    return parent;
  }

  public void setParent(long parent) {
    this.parent = parent;
  }

  public long getOrder() {
    return order;
  }

  public void setOrder(long order) {
    this.order = order;
  }

  public boolean isMoved() {
    return moved;
  }

  public void setMoved(boolean moved) {
    this.moved = moved;
  }

  public long getRemoteOrder() {
    return remoteOrder;
  }

  public void setRemoteOrder(long remoteOrder) {
    this.remoteOrder = remoteOrder;
  }

  public long getLastSync() {
    return lastSync;
  }

  public void setLastSync(long lastSync) {
    this.lastSync = lastSync;
  }

  public long getDeleted() {
    return deleted;
  }

  public void setDeleted(long deleted) {
    this.deleted = deleted;
  }

  public String getRemoteParent() {
    return remoteParent;
  }

  public void setRemoteParent(String remoteParent) {
    this.remoteParent = remoteParent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GoogleTask)) {
      return false;
    }
    GoogleTask that = (GoogleTask) o;
    return id == that.id
        && task == that.task
        && parent == that.parent
        && moved == that.moved
        && order == that.order
        && remoteOrder == that.remoteOrder
        && lastSync == that.lastSync
        && deleted == that.deleted
        && Objects.equals(remoteId, that.remoteId)
        && Objects.equals(listId, that.listId)
        && Objects.equals(remoteParent, that.remoteParent);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, task, remoteId, listId, parent, remoteParent, moved, order, remoteOrder, lastSync,
            deleted);
  }

  @Override
  public String toString() {
    return "GoogleTask{"
        + "id="
        + id
        + ", task="
        + task
        + ", remoteId='"
        + remoteId
        + '\''
        + ", listId='"
        + listId
        + '\''
        + ", parent="
        + parent
        + ", moved="
        + moved
        + ", order="
        + order
        + ", remoteParent='"
        + remoteParent
        + '\''
        + ", remoteOrder="
        + remoteOrder
        + ", lastSync="
        + lastSync
        + ", deleted="
        + deleted
        + '}';
  }
}
