package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.DateUtilities;

@Entity(tableName = "google_tasks")
public class GoogleTask {

  public static final String KEY = "gtasks"; // $NON-NLS-1$

  @Deprecated public static final Table TABLE = new Table("google_tasks");

  @Deprecated
  public static final Property.IntegerProperty INDENT =
      new Property.IntegerProperty(GoogleTask.TABLE, "indent");

  @Deprecated
  public static final Property.IntegerProperty ORDER =
      new Property.IntegerProperty(GoogleTask.TABLE, "`order`").as("gtasks_order");

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient long id;

  @ColumnInfo(name = "task")
  private transient long task;

  @ColumnInfo(name = "remote_id")
  private String remoteId = "";

  @ColumnInfo(name = "list_id")
  private String listId = "";

  @ColumnInfo(name = "parent")
  private long parent;

  @ColumnInfo(name = "indent")
  private int indent;

  @ColumnInfo(name = "order")
  private long order;

  @ColumnInfo(name = "remote_order")
  private long remoteOrder;

  @ColumnInfo(name = "last_sync")
  private long lastSync;

  @ColumnInfo(name = "deleted")
  private long deleted;

  @Ignore private transient boolean suppressSync;

  public GoogleTask() {}

  @Ignore
  public GoogleTask(long task, String listId) {
    this.task = task;
    this.order = DateUtilities.now();
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

  public int getIndent() {
    return indent;
  }

  public void setIndent(int indent) {
    this.indent = indent;
  }

  public long getOrder() {
    return order;
  }

  public void setOrder(long order) {
    this.order = order;
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

  public boolean isSuppressSync() {
    return suppressSync;
  }

  public void setSuppressSync(boolean suppressSync) {
    this.suppressSync = suppressSync;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GoogleTask that = (GoogleTask) o;

    if (id != that.id) {
      return false;
    }
    if (task != that.task) {
      return false;
    }
    if (parent != that.parent) {
      return false;
    }
    if (indent != that.indent) {
      return false;
    }
    if (order != that.order) {
      return false;
    }
    if (remoteOrder != that.remoteOrder) {
      return false;
    }
    if (lastSync != that.lastSync) {
      return false;
    }
    if (deleted != that.deleted) {
      return false;
    }
    if (suppressSync != that.suppressSync) {
      return false;
    }
    if (remoteId != null ? !remoteId.equals(that.remoteId) : that.remoteId != null) {
      return false;
    }
    return listId != null ? listId.equals(that.listId) : that.listId == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (task ^ (task >>> 32));
    result = 31 * result + (remoteId != null ? remoteId.hashCode() : 0);
    result = 31 * result + (listId != null ? listId.hashCode() : 0);
    result = 31 * result + (int) (parent ^ (parent >>> 32));
    result = 31 * result + indent;
    result = 31 * result + (int) (order ^ (order >>> 32));
    result = 31 * result + (int) (remoteOrder ^ (remoteOrder >>> 32));
    result = 31 * result + (int) (lastSync ^ (lastSync >>> 32));
    result = 31 * result + (int) (deleted ^ (deleted >>> 32));
    result = 31 * result + (suppressSync ? 1 : 0);
    return result;
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
        + ", indent="
        + indent
        + ", order="
        + order
        + ", remoteOrder="
        + remoteOrder
        + ", lastSync="
        + lastSync
        + ", deleted="
        + deleted
        + '}';
  }
}
