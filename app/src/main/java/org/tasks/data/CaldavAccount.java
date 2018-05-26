package org.tasks.data;

import static com.todoroo.astrid.data.Task.NO_UUID;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "caldav_account")
public class CaldavAccount implements Parcelable {

  public static Parcelable.Creator<CaldavAccount> CREATOR =
      new Parcelable.Creator<CaldavAccount>() {

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

  @ColumnInfo(name = "url")
  private String url = "";

  @ColumnInfo(name = "username")
  private String username = "";

  @ColumnInfo(name = "password")
  private transient String password = "";

  @ColumnInfo(name = "error")
  private transient String error = "";

  public CaldavAccount() {}

  @Ignore
  public CaldavAccount(Parcel source) {
    id = source.readLong();
    uuid = source.readString();
    name = source.readString();
    url = source.readString();
    username = source.readString();
    password = source.readString();
    error = source.readString();
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

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  @Override
  public String toString() {
    return "CaldavAccount{"
        + "id="
        + id
        + ", uuid='"
        + uuid
        + '\''
        + ", name='"
        + name
        + '\''
        + ", url='"
        + url
        + '\''
        + ", username='"
        + username
        + '\''
        + ", password='"
        + password
        + '\''
        + ", error='"
        + error
        + '\''
        + '}';
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
    if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) {
      return false;
    }
    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (url != null ? !url.equals(that.url) : that.url != null) {
      return false;
    }
    if (username != null ? !username.equals(that.username) : that.username != null) {
      return false;
    }
    if (password != null ? !password.equals(that.password) : that.password != null) {
      return false;
    }
    return error != null ? error.equals(that.error) : that.error == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (username != null ? username.hashCode() : 0);
    result = 31 * result + (password != null ? password.hashCode() : 0);
    result = 31 * result + (error != null ? error.hashCode() : 0);
    return result;
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
    dest.writeString(url);
    dest.writeString(username);
    dest.writeString(password);
    dest.writeString(error);
  }
}
