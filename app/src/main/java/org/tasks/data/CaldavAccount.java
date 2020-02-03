package org.tasks.data;

import static com.todoroo.astrid.data.Task.NO_UUID;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.os.ParcelCompat;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.tasks.security.Encryption;

@Entity(tableName = "caldav_accounts")
public class CaldavAccount implements Parcelable {

  public static final int TYPE_CALDAV = 0;
  public static final int TYPE_ETESYNC = 1;
  public static final Parcelable.Creator<CaldavAccount> CREATOR =
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
  @ColumnInfo(name = "cda_id")
  private long id;

  @ColumnInfo(name = "cda_uuid")
  private String uuid = NO_UUID;

  @ColumnInfo(name = "cda_name")
  private String name = "";

  @ColumnInfo(name = "cda_url")
  private String url = "";

  @ColumnInfo(name = "cda_username")
  private String username = "";

  @ColumnInfo(name = "cda_password")
  private transient String password = "";

  @ColumnInfo(name = "cda_error")
  private transient String error = "";

  @ColumnInfo(name = "cda_repeat")
  private boolean suppressRepeatingTasks;

  @ColumnInfo(name = "cda_encryption_key")
  private transient String encryptionKey;

  @ColumnInfo(name = "cda_account_type")
  private int accountType;

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
    suppressRepeatingTasks = ParcelCompat.readBoolean(source);
    accountType = source.readInt();
    encryptionKey = source.readString();
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

  public String getPassword(Encryption encryption) {
    return encryption.decrypt(password);
  }

  public String getEncryptionKey() {
    return encryptionKey;
  }

  public void setEncryptionKey(String encryptionKey) {
    this.encryptionKey = encryptionKey;
  }

  public String getEncryptionPassword(Encryption encryption) {
    return encryption.decrypt(encryptionKey);
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public boolean isSuppressRepeatingTasks() {
    return suppressRepeatingTasks;
  }

  public void setSuppressRepeatingTasks(boolean suppressRepeatingTasks) {
    this.suppressRepeatingTasks = suppressRepeatingTasks;
  }

  public int getAccountType() {
    return accountType;
  }

  public void setAccountType(int accountType) {
    this.accountType = accountType;
  }

  public boolean isCaldavAccount() {
    return accountType == TYPE_CALDAV;
  }

  public boolean isEteSyncAccount() {
    return accountType == TYPE_ETESYNC;
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
        + ", suppressRepeatingTasks="
        + suppressRepeatingTasks
        + ", encryptionKey='"
        + encryptionKey
        + '\''
        + ", accountType="
        + accountType
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
    if (suppressRepeatingTasks != that.suppressRepeatingTasks) {
      return false;
    }
    if (accountType != that.accountType) {
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
    if (error != null ? !error.equals(that.error) : that.error != null) {
      return false;
    }
    return encryptionKey != null
        ? encryptionKey.equals(that.encryptionKey)
        : that.encryptionKey == null;
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
    result = 31 * result + (suppressRepeatingTasks ? 1 : 0);
    result = 31 * result + (encryptionKey != null ? encryptionKey.hashCode() : 0);
    result = 31 * result + accountType;
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
    ParcelCompat.writeBoolean(dest, suppressRepeatingTasks);
    dest.writeInt(accountType);
    dest.writeString(encryptionKey);
  }
}
