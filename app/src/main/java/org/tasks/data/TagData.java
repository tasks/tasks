package org.tasks.data;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.todoroo.astrid.data.Task;
import org.tasks.backup.XmlReader;
import org.tasks.themes.CustomIcons;

@Entity(tableName = "tagdata")
public final class TagData implements Parcelable {

  public static final Parcelable.Creator<TagData> CREATOR =
      new Parcelable.Creator<TagData>() {
        @Override
        public TagData createFromParcel(Parcel source) {
          return new TagData(source);
        }

        @Override
        public TagData[] newArray(int size) {
          return new TagData[size];
        }
      };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient Long id;

  @ColumnInfo(name = "remoteId")
  private String remoteId = Task.NO_UUID;

  @ColumnInfo(name = "name")
  private String name = "";

  @ColumnInfo(name = "color")
  private Integer color = -1;

  @ColumnInfo(name = "tagOrdering")
  private String tagOrdering = "[]";

  @ColumnInfo(name = "td_icon")
  private Integer icon = -1;

  @Ignore
  public TagData(String name) {
    this.name = name;
  }

  public TagData() {}

  @Ignore
  public TagData(XmlReader reader) {
    reader.readString("remoteId", this::setRemoteId);
    reader.readString("name", this::setName);
    reader.readInteger("color", this::setColor);
    reader.readString("tagOrdering", this::setTagOrdering);
  }

  @SuppressLint("ParcelClassLoader")
  @Ignore
  private TagData(Parcel parcel) {
    id = (Long) parcel.readValue(null);
    remoteId = parcel.readString();
    name = parcel.readString();
    color = parcel.readInt();
    tagOrdering = parcel.readString();
    icon = parcel.readInt();
  }

  public Long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTagOrdering() {
    return tagOrdering;
  }

  public void setTagOrdering(String tagOrdering) {
    this.tagOrdering = tagOrdering;
  }

  public Integer getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public Integer getIcon() {
    return icon == null ? CustomIcons.getLABEL() : icon;
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
    dest.writeValue(id);
    dest.writeString(remoteId);
    dest.writeString(name);
    dest.writeInt(color);
    dest.writeString(tagOrdering);
    dest.writeInt(getIcon());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TagData)) {
      return false;
    }

    TagData tagData = (TagData) o;

    if (id != null ? !id.equals(tagData.id) : tagData.id != null) {
      return false;
    }
    if (remoteId != null ? !remoteId.equals(tagData.remoteId) : tagData.remoteId != null) {
      return false;
    }
    if (name != null ? !name.equals(tagData.name) : tagData.name != null) {
      return false;
    }
    if (color != null ? !color.equals(tagData.color) : tagData.color != null) {
      return false;
    }
    if (tagOrdering != null
        ? !tagOrdering.equals(tagData.tagOrdering)
        : tagData.tagOrdering != null) {
      return false;
    }
    return icon != null ? icon.equals(tagData.icon) : tagData.icon == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (remoteId != null ? remoteId.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (color != null ? color.hashCode() : 0);
    result = 31 * result + (tagOrdering != null ? tagOrdering.hashCode() : 0);
    result = 31 * result + (icon != null ? icon.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TagData{"
        + "id="
        + id
        + ", remoteId='"
        + remoteId
        + '\''
        + ", name='"
        + name
        + '\''
        + ", color="
        + color
        + ", icon="
        + icon
        + ", tagOrdering='"
        + tagOrdering
        + '\''
        + '}';
  }
}
