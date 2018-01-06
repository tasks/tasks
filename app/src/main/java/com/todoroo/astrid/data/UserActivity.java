package com.todoroo.astrid.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;
import org.tasks.backup.XmlReader;
import org.tasks.backup.XmlWriter;

import java.io.File;

import timber.log.Timber;

@Entity(tableName = "userActivity")
public class UserActivity implements Parcelable {

    public static final String ACTION_TASK_COMMENT = "task_comment";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @ColumnInfo(name = "remoteId")
    private String remoteId = RemoteModel.NO_UUID;

    @ColumnInfo(name = "action")
    private String action = "";

    @ColumnInfo(name = "message")
    private String message = "";

    @ColumnInfo(name = "picture")
    private String picture = "";

    @ColumnInfo(name = "target_id")
    private String targetId = RemoteModel.NO_UUID;

    @ColumnInfo(name = "created_at")
    private Long created = 0L;

    @ColumnInfo(name = "deleted_at")
    private Long deleted = 0L;

    public UserActivity() {
    }

    @Ignore
    public UserActivity(XmlReader reader) {
        reader.readString("remoteId", this::setRemoteId);
        reader.readString("action", this::setAction);
        reader.readString("message", this::setMessage);
        reader.readString("picture", this::setPicture);
        reader.readString("target_id", this::setTargetId);
        reader.readLong("created_at", this::setCreated);
        reader.readLong("deleted_at", this::setDeleted);
    }

    @Ignore
    private UserActivity(Parcel parcel) {
        id = parcel.readLong();
        remoteId = parcel.readString();
        action = parcel.readString();
        message = parcel.readString();
        picture = parcel.readString();
        targetId = parcel.readString();
        created = parcel.readLong();
        deleted = parcel.readLong();
    }

    public void writeToXml(XmlWriter writer) {
        writer.writeString("remoteId", remoteId);
        writer.writeString("action", action);
        writer.writeString("message", message);
        writer.writeString("picture", picture);
        writer.writeString("target_id", targetId);
        writer.writeLong("created_at", created);
        writer.writeLong("deleted_at", deleted);
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }

    public Uri getPictureUri() {
        return getPictureUri(picture);
    }

    public static final Creator<UserActivity> CREATOR = new Creator<UserActivity>() {
        @Override
        public UserActivity createFromParcel(Parcel source) {
            return new UserActivity(source);
        }

        @Override
        public UserActivity[] newArray(int size) {
            return new UserActivity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(remoteId);
        dest.writeString(action);
        dest.writeString(message);
        dest.writeString(picture);
        dest.writeString(targetId);
        dest.writeLong(created);
        dest.writeLong(deleted);
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
            Timber.e(e, e.getMessage());
            return null;
        }
    }
}
