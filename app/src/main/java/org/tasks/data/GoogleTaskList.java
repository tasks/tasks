package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "google_task_lists")
public class GoogleTaskList implements Parcelable {

  public static Creator<GoogleTaskList> CREATOR =
      new Creator<GoogleTaskList>() {
        @Override
        public GoogleTaskList createFromParcel(Parcel parcel) {
          return new GoogleTaskList(parcel);
        }

        @Override
        public GoogleTaskList[] newArray(int size) {
          return new GoogleTaskList[size];
        }
      };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient long id;

  @ColumnInfo(name = "account")
  private String account;

  @ColumnInfo(name = "remote_id")
  private String remoteId;

  @ColumnInfo(name = "title")
  private String title;

  @ColumnInfo(name = "remote_order")
  private int remoteOrder;

  @ColumnInfo(name = "last_sync")
  private long lastSync;

  @ColumnInfo(name = "deleted")
  @Deprecated
  private long deleted = 0;

  @ColumnInfo(name = "color")
  private Integer color;

  public GoogleTaskList() {}

  @Ignore
  public GoogleTaskList(Parcel parcel) {
    id = parcel.readLong();
    account = parcel.readString();
    remoteId = parcel.readString();
    title = parcel.readString();
    remoteOrder = parcel.readInt();
    lastSync = parcel.readLong();
    deleted = parcel.readLong();
    color = parcel.readInt();
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getRemoteOrder() {
    return remoteOrder;
  }

  public void setRemoteOrder(int remoteOrder) {
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

  public Integer getColor() {
    return color == null ? -1 : color;
  }

  public void setColor(Integer color) {
    this.color = color;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GoogleTaskList)) {
      return false;
    }

    GoogleTaskList that = (GoogleTaskList) o;

    if (id != that.id) {
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
    if (account != null ? !account.equals(that.account) : that.account != null) {
      return false;
    }
    if (remoteId != null ? !remoteId.equals(that.remoteId) : that.remoteId != null) {
      return false;
    }
    if (title != null ? !title.equals(that.title) : that.title != null) {
      return false;
    }
    return color != null ? color.equals(that.color) : that.color == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (account != null ? account.hashCode() : 0);
    result = 31 * result + (remoteId != null ? remoteId.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + remoteOrder;
    result = 31 * result + (int) (lastSync ^ (lastSync >>> 32));
    result = 31 * result + (int) (deleted ^ (deleted >>> 32));
    result = 31 * result + (color != null ? color.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "GoogleTaskList{"
        + "id="
        + id
        + ", account='"
        + account
        + '\''
        + ", remoteId='"
        + remoteId
        + '\''
        + ", title='"
        + title
        + '\''
        + ", remoteOrder="
        + remoteOrder
        + ", lastSync="
        + lastSync
        + ", deleted="
        + deleted
        + ", color="
        + color
        + '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeLong(id);
    parcel.writeString(account);
    parcel.writeString(remoteId);
    parcel.writeString(title);
    parcel.writeInt(remoteOrder);
    parcel.writeLong(lastSync);
    parcel.writeLong(deleted);
    parcel.writeInt(getColor());
  }
}
