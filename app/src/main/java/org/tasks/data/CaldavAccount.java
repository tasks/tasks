package org.tasks.data;

import static com.todoroo.astrid.data.Task.NO_UUID;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.os.ParcelCompat;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.Objects;
import org.tasks.security.KeyStoreEncryption;

@Entity(tableName = "caldav_accounts")
public class CaldavAccount implements Parcelable {

  private static final int TYPE_CALDAV = 0;
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

  @ColumnInfo(name = "cda_collapsed")
  private boolean collapsed;

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
    collapsed = ParcelCompat.readBoolean(source);
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

  public String getPassword(KeyStoreEncryption encryption) {
    return encryption.decrypt(password);
  }

  public String getEncryptionKey() {
    return encryptionKey;
  }

  public void setEncryptionKey(String encryptionKey) {
    this.encryptionKey = encryptionKey;
  }

  public String getEncryptionPassword(KeyStoreEncryption encryption) {
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

  public boolean isCollapsed() {
    return collapsed;
  }

  public void setCollapsed(boolean collapsed) {
    this.collapsed = collapsed;
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
        + ", collapsed="
        + collapsed
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
    return id == that.id
        && suppressRepeatingTasks == that.suppressRepeatingTasks
        && accountType == that.accountType
        && collapsed == that.collapsed
        && Objects.equals(uuid, that.uuid)
        && Objects.equals(name, that.name)
        && Objects.equals(url, that.url)
        && Objects.equals(username, that.username)
        && Objects.equals(password, that.password)
        && Objects.equals(error, that.error)
        && Objects.equals(encryptionKey, that.encryptionKey);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, uuid, name, url, username, password, error, suppressRepeatingTasks, encryptionKey,
            accountType, collapsed);
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
    ParcelCompat.writeBoolean(dest, collapsed);
  }
}
