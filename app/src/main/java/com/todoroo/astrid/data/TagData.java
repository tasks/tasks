package com.todoroo.astrid.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

import org.tasks.backup.XmlReader;
import org.tasks.backup.XmlWriter;

import java.io.Serializable;

@Entity(tableName = "tagdata")
public final class TagData implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @ColumnInfo(name = "remoteId")
    private String remoteId = RemoteModel.NO_UUID;

    @ColumnInfo(name = "name")
    private String name = "";

    @ColumnInfo(name = "color")
    private Integer color = -1;

    @ColumnInfo(name = "tagOrdering")
    private String tagOrdering = "[]";

    @Deprecated
    @ColumnInfo(name = "deleted")
    private Long deleted = 0L;

    public TagData() {
    }

    @Ignore
    public TagData(XmlReader reader) {
        reader.readString("remoteId", this::setRemoteId);
        reader.readString("name", this::setName);
        reader.readInteger("color", this::setColor);
        reader.readString("tagOrdering", this::setTagOrdering);
        reader.readLong("deleted", this::setDeleted);
    }

    @Ignore
    private TagData(Parcel parcel) {
        id = parcel.readLong();
        remoteId = parcel.readString();
        name = parcel.readString();
        color = parcel.readInt();
        tagOrdering = parcel.readString();
        deleted = parcel.readLong();
    }

    public void writeToXml(XmlWriter writer) {
        writer.writeString("remoteId", remoteId);
        writer.writeString("name", name);
        writer.writeInteger("color", color);
        writer.writeString("tagOrdering", tagOrdering);
        writer.writeLong("deleted", deleted);
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

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public static final Creator<TagData> CREATOR = new Creator<TagData>() {
        @Override
        public TagData createFromParcel(Parcel source) {
            return new TagData(source);
        }

        @Override
        public TagData[] newArray(int size) {
            return new TagData[size];
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
        dest.writeString(name);
        dest.writeInt(color);
        dest.writeString(tagOrdering);
        dest.writeLong(deleted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagData tagData = (TagData) o;

        if (id != null ? !id.equals(tagData.id) : tagData.id != null) return false;
        if (remoteId != null ? !remoteId.equals(tagData.remoteId) : tagData.remoteId != null)
            return false;
        if (name != null ? !name.equals(tagData.name) : tagData.name != null) return false;
        if (color != null ? !color.equals(tagData.color) : tagData.color != null) return false;
        if (tagOrdering != null ? !tagOrdering.equals(tagData.tagOrdering) : tagData.tagOrdering != null)
            return false;
        return deleted != null ? deleted.equals(tagData.deleted) : tagData.deleted == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (remoteId != null ? remoteId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (tagOrdering != null ? tagOrdering.hashCode() : 0);
        result = 31 * result + (deleted != null ? deleted.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TagData{" +
                "id=" + id +
                ", remoteId='" + remoteId + '\'' +
                ", name='" + name + '\'' +
                ", color=" + color +
                ", tagOrdering='" + tagOrdering + '\'' +
                ", deleted=" + deleted +
                '}';
    }
}
