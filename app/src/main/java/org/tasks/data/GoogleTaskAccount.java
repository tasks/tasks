package org.tasks.data;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "google_task_accounts")
public class GoogleTaskAccount implements Parcelable {
  public static final Creator<GoogleTaskAccount> CREATOR =
      new Creator<GoogleTaskAccount>() {
        @Override
        public GoogleTaskAccount createFromParcel(Parcel source) {
          return new GoogleTaskAccount(source);
        }

        @Override
        public GoogleTaskAccount[] newArray(int size) {
          return new GoogleTaskAccount[size];
        }
      };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient long id;

  @ColumnInfo(name = "account")
  private String account;

  @ColumnInfo(name = "error")
  private transient String error = "";

  @ColumnInfo(name = "etag")
  private String etag;

  public GoogleTaskAccount() {}

  @Ignore
  public GoogleTaskAccount(Parcel source) {
    id = source.readLong();
    account = source.readString();
    error = source.readString();
    etag = source.readString();
  }

  @Ignore
  public GoogleTaskAccount(String account) {
    this.account = account;
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

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GoogleTaskAccount that = (GoogleTaskAccount) o;

    if (id != that.id) {
      return false;
    }
    if (account != null ? !account.equals(that.account) : that.account != null) {
      return false;
    }
    if (error != null ? !error.equals(that.error) : that.error != null) {
      return false;
    }
    return etag != null ? etag.equals(that.etag) : that.etag == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (account != null ? account.hashCode() : 0);
    result = 31 * result + (error != null ? error.hashCode() : 0);
    result = 31 * result + (etag != null ? etag.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "GoogleTaskAccount{"
        + "id="
        + id
        + ", account='"
        + account
        + '\''
        + ", error='"
        + error
        + '\''
        + ", etag='"
        + etag
        + '\''
        + '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(account);
    dest.writeString(error);
    dest.writeString(etag);
  }
}
