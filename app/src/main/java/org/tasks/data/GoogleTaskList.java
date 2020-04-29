package org.tasks.data;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.Objects;
import org.tasks.themes.CustomIcons;

@Entity(tableName = "google_task_lists")
public class GoogleTaskList implements Parcelable {

  public static final Parcelable.Creator<GoogleTaskList> CREATOR =
      new Parcelable.Creator<GoogleTaskList>() {
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
  @ColumnInfo(name = "gtl_id")
  private transient long id;

  @ColumnInfo(name = "gtl_account")
  private String account;

  @ColumnInfo(name = "gtl_remote_id")
  private String remoteId;

  @ColumnInfo(name = "gtl_title")
  private String title;

  @ColumnInfo(name = "gtl_remote_order")
  private int remoteOrder;

  @ColumnInfo(name = "gtl_last_sync")
  private long lastSync;

  @ColumnInfo(name = "gtl_color")
  private Integer color;

  @ColumnInfo(name = "gtl_icon")
  private Integer icon = -1;

  public GoogleTaskList() {}

  @Ignore
  public GoogleTaskList(Parcel parcel) {
    id = parcel.readLong();
    account = parcel.readString();
    remoteId = parcel.readString();
    title = parcel.readString();
    remoteOrder = parcel.readInt();
    lastSync = parcel.readLong();
    color = parcel.readInt();
    icon = parcel.readInt();
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

  public Integer getColor() {
    return color == null ? 0 : color;
  }

  public void setColor(Integer color) {
    this.color = color;
  }

  public Integer getIcon() {
    return icon == null ? CustomIcons.getCLOUD() : icon;
  }

  public void setIcon(Integer icon) {
    this.icon = icon;
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
    parcel.writeInt(getColor());
    parcel.writeInt(getIcon());
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
    return id == that.id
        && remoteOrder == that.remoteOrder
        && lastSync == that.lastSync
        && Objects.equals(account, that.account)
        && Objects.equals(remoteId, that.remoteId)
        && Objects.equals(title, that.title)
        && Objects.equals(color, that.color)
        && Objects.equals(icon, that.icon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, account, remoteId, title, remoteOrder, lastSync, color, icon);
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
        + ", color="
        + color
        + ", icon="
        + icon
        + '}';
  }
}
