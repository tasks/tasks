package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.astrid.data.Task;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;
import org.tasks.backup.XmlReader;
import timber.log.Timber;

@Entity(tableName = "userActivity")
public class UserActivity implements Parcelable {

  public static final Creator<UserActivity> CREATOR =
      new Creator<UserActivity>() {
        @Override
        public UserActivity createFromParcel(Parcel source) {
          return new UserActivity(source);
        }

        @Override
        public UserActivity[] newArray(int size) {
          return new UserActivity[size];
        }
      };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient Long id;

  @ColumnInfo(name = "remoteId")
  private String remoteId = Task.NO_UUID;

  @ColumnInfo(name = "message")
  private String message = "";

  @ColumnInfo(name = "picture")
  private String picture = "";

  @ColumnInfo(name = "target_id")
  private transient String targetId = Task.NO_UUID;

  @ColumnInfo(name = "created_at")
  private Long created = 0L;

  public UserActivity() {}

  @Ignore
  public UserActivity(XmlReader reader) {
    reader.readString("remoteId", this::setRemoteId);
    reader.readString("message", this::setMessage);
    reader.readString("picture", this::setPicture);
    reader.readString("target_id", this::setTargetId);
    reader.readLong("created_at", this::setCreated);
  }

  @Ignore
  private UserActivity(Parcel parcel) {
    id = parcel.readLong();
    remoteId = parcel.readString();
    message = parcel.readString();
    picture = parcel.readString();
    targetId = parcel.readString();
    created = parcel.readLong();
  }

  private static Uri getPictureUri(String value) {
    try {
      if (value == null) {
        return null;
      }
      if (value.contains("uri") || value.contains("path")) {
        JSONObject json = new JSONObject(value);
        if (json.has("uri")) {
          return Uri.parse(json.getString("uri"));
        }
        if (json.has("path")) {
          String path = json.getString("path");
          return Uri.fromFile(new File(path));
        }
      }
      return null;
    } catch (JSONException e) {
      Timber.e(e);
      return null;
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getPicture() {
    return picture;
  }

  public void setPicture(String picture) {
    this.picture = picture;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public Long getCreated() {
    return created;
  }

  public void setCreated(Long created) {
    this.created = created;
  }

  public Uri getPictureUri() {
    return getPictureUri(picture);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(id);
    dest.writeString(remoteId);
    dest.writeString(message);
    dest.writeString(picture);
    dest.writeString(targetId);
    dest.writeLong(created);
  }
}
