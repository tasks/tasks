package org.tasks.data;


import static com.todoroo.astrid.data.Task.NO_UUID;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

@Entity(tableName = "caldav_account")
public final class CaldavAccount implements Parcelable {

  public static Parcelable.Creator<CaldavAccount> CREATOR = new Parcelable.Creator<CaldavAccount>() {
    @Override
    public CaldavAccount createFromParcel(Parcel source) {
      return new CaldavAccount(source);
    }

    @Override
    public CaldavAccount[] newArray(int size) {
      return new CaldavAccount[size];
    }
  };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private long id;
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
  @ColumnInfo(name = "username")
  private String username = "";
  @ColumnInfo(name = "password")
  private transient String password = "";

  public CaldavAccount() {

  }

  @Ignore
  public CaldavAccount(String name, String uuid) {
    this.name = name;
    this.uuid = uuid;
  }

  @Ignore
  public CaldavAccount(Parcel source) {
    id = source.readLong();
    uuid = source.readString();
    name = source.readString();
    color = source.readInt();
    ctag = source.readString();
    url = source.readString();
    username = source.readString();
    password = source.readString();
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
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

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(uuid);
    dest.writeString(name);
    dest.writeInt(color);
    dest.writeString(ctag);
    dest.writeString(url);
    dest.writeString(username);
    dest.writeString(password);
  }

  @Override
  public String toString() {
    return "CaldavAccount{" +
        "id=" + id +
        ", uuid='" + uuid + '\'' +
        ", name='" + name + '\'' +
        ", color=" + color +
        ", ctag='" + ctag + '\'' +
        ", url='" + url + '\'' +
        ", username='" + username + '\'' +
        ", password='" + (TextUtils.isEmpty(password) ? "null" : "******") + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CaldavAccount)) {
      return false;
    }

    CaldavAccount that = (CaldavAccount) o;

    if (id != that.id) {
      return false;
    }
    if (color != that.color) {
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
    if (username != null ? !username.equals(that.username) : that.username != null) {
      return false;
    }
    return password != null ? password.equals(that.password) : that.password == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + color;
    result = 31 * result + (ctag != null ? ctag.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (username != null ? username.hashCode() : 0);
    result = 31 * result + (password != null ? password.hashCode() : 0);
    return result;
  }
}
