package org.tasks.data;

import static com.todoroo.astrid.data.Task.NO_UUID;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.tasks.themes.CustomIcons;

@Entity(tableName = "caldav_lists")
public final class CaldavCalendar implements Parcelable {

  public static final Parcelable.Creator<CaldavCalendar> CREATOR =
      new Parcelable.Creator<CaldavCalendar>() {
        @Override
        public CaldavCalendar createFromParcel(Parcel source) {
          return new CaldavCalendar(source);
        }

        @Override
        public CaldavCalendar[] newArray(int size) {
          return new CaldavCalendar[size];
        }
      };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "cdl_id")
  private long id;

  @ColumnInfo(name = "cdl_account")
  private String account = NO_UUID;

  @ColumnInfo(name = "cdl_uuid")
  private String uuid = NO_UUID;

  @ColumnInfo(name = "cdl_name")
  private String name = "";

  @ColumnInfo(name = "cdl_color")
  private int color = -1;

  @ColumnInfo(name = "cdl_ctag")
  private String ctag;

  @ColumnInfo(name = "cdl_url")
  private String url = "";

  @ColumnInfo(name = "cdl_icon")
  private Integer icon = -1;

  public CaldavCalendar() {}

  @Ignore
  public CaldavCalendar(String name, String uuid) {
    this.name = name;
    this.uuid = uuid;
  }

  @Ignore
  public CaldavCalendar(Parcel source) {
    id = source.readLong();
    account = source.readString();
    uuid = source.readString();
    name = source.readString();
    color = source.readInt();
    ctag = source.readString();
    url = source.readString();
    icon = source.readInt();
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

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public String getCtag() {
    return ctag;
  }

  public void setCtag(String ctag) {
    this.ctag = ctag;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
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
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(account);
    dest.writeString(uuid);
    dest.writeString(name);
    dest.writeInt(color);
    dest.writeString(ctag);
    dest.writeString(url);
    dest.writeInt(getIcon());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CaldavCalendar)) {
      return false;
    }

    CaldavCalendar that = (CaldavCalendar) o;

    if (id != that.id) {
      return false;
    }
    if (color != that.color) {
      return false;
    }
    if (account != null ? !account.equals(that.account) : that.account != null) {
      return false;
    }
    if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
      return false;
    }
    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (ctag != null ? !ctag.equals(that.ctag) : that.ctag != null) {
      return false;
    }
    if (url != null ? !url.equals(that.url) : that.url != null) {
      return false;
    }
    return icon != null ? icon.equals(that.icon) : that.icon == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (account != null ? account.hashCode() : 0);
    result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + color;
    result = 31 * result + (ctag != null ? ctag.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (icon != null ? icon.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "CaldavCalendar{"
        + "id="
        + id
        + ", account='"
        + account
        + '\''
        + ", uuid='"
        + uuid
        + '\''
        + ", name='"
        + name
        + '\''
        + ", color="
        + color
        + ", ctag='"
        + ctag
        + '\''
        + ", url='"
        + url
        + '\''
        + ", icon="
        + icon
        + '}';
  }
}
