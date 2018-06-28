package org.tasks.data;

import static com.todoroo.astrid.data.Task.NO_UUID;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "caldav_calendar")
public final class CaldavCalendar implements Parcelable {

  public static Parcelable.Creator<CaldavCalendar> CREATOR =
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
  @ColumnInfo(name = "_id")
  private long id;

  @ColumnInfo(name = "account")
  private String account = NO_UUID;

  @ColumnInfo(name = "uuid")
  private String uuid = NO_UUID;

  @ColumnInfo(name = "name")
  private String name = "";

  @ColumnInfo(name = "color")
  private int color = -1;

  @ColumnInfo(name = "ctag")
  private String ctag;

  @ColumnInfo(name = "url")
  private String url = "";

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
        + '}';
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
    return url != null ? url.equals(that.url) : that.url == null;
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
    return result;
  }
}
