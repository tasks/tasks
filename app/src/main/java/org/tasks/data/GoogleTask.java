package org.tasks.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;

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

  public static final Property.IntegerProperty ORDER =
      new Property.IntegerProperty(GoogleTask.TABLE, "gt_order");

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

  @ColumnInfo(name = "gt_email_description")
  private String emailDescription;

  @ColumnInfo(name = "gt_email_url")
  private String emailUrl;

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

  public int getIndent() {
    return parent > 0 ? 1 : 0;
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

  public String getEmailDescription() {
    return emailDescription;
  }

  public void setEmailDescription(String emailDescription) {
    this.emailDescription = emailDescription;
  }

  public String getEmailUrl() {
    return emailUrl;
  }

  public void setEmailUrl(String emailUrl) {
    this.emailUrl = emailUrl;
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

    if (id != that.id) {
      return false;
    }
    if (task != that.task) {
      return false;
    }
    if (parent != that.parent) {
      return false;
    }
    if (moved != that.moved) {
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
    if (remoteId != null ? !remoteId.equals(that.remoteId) : that.remoteId != null) {
      return false;
    }
    if (listId != null ? !listId.equals(that.listId) : that.listId != null) {
      return false;
    }
    if (remoteParent != null
        ? !remoteParent.equals(that.remoteParent)
        : that.remoteParent != null) {
      return false;
    }
    if (emailDescription != null
        ? !emailDescription.equals(that.emailDescription)
        : that.emailDescription != null) {
      return false;
    }
    return emailUrl != null
        ? emailUrl.equals(that.emailUrl)
        : that.emailUrl == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (task ^ (task >>> 32));
    result = 31 * result + (remoteId != null ? remoteId.hashCode() : 0);
    result = 31 * result + (listId != null ? listId.hashCode() : 0);
    result = 31 * result + (int) (parent ^ (parent >>> 32));
    result = 31 * result + (remoteParent != null ? remoteParent.hashCode() : 0);
    result = 31 * result + (moved ? 1 : 0);
    result = 31 * result + (int) (order ^ (order >>> 32));
    result = 31 * result + (int) (remoteOrder ^ (remoteOrder >>> 32));
    result = 31 * result + (int) (lastSync ^ (lastSync >>> 32));
    result = 31 * result + (int) (deleted ^ (deleted >>> 32));
    result =
        31 * result + (emailDescription != null ? emailDescription.hashCode() : 0);
    result = 31 * result + (emailUrl != null ? emailUrl.hashCode() : 0);
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
        + ", remoteParent='"
        + remoteParent
        + '\''
        + ", moved="
        + moved
        + ", order="
        + order
        + ", remoteOrder="
        + remoteOrder
        + ", lastSync="
        + lastSync
        + ", deleted="
        + deleted
        + ", relatedEmailDescription='"
        + emailDescription
        + '\''
        + ", relatedEmailUrl='"
        + emailUrl
        + '\''
        + '}';
  }
}
