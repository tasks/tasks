/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "store",
        indices = @Index(name = "so_id", value = {"type", "item"}))
public class StoreObject implements Parcelable{

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @ColumnInfo(name = "type")
    private String type;

    @ColumnInfo(name = "item")
    private String item;

    @ColumnInfo(name = "value")
    private String value;

    @ColumnInfo(name = "value2")
    private String value2;

    @ColumnInfo(name = "value3")
    private String value3;

    @ColumnInfo(name = "value4")
    private String value4;

    @ColumnInfo(name = "deleted")
    private Long deleted = 0L;

    public StoreObject() {

    }

    @Ignore
    public StoreObject(Parcel source) {
        id = source.readLong();
        type = source.readString();
        item = source.readString();
        value = source.readString();
        value2 = source.readString();
        value3 = source.readString();
        value4 = source.readString();
        deleted = source.readLong();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public String getValue3() {
        return value3;
    }

    public void setValue3(String value3) {
        this.value3 = value3;
    }

    public String getValue4() {
        return value4;
    }

    public void setValue4(String value4) {
        this.value4 = value4;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof StoreObject)) return false;

        StoreObject that = (StoreObject) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (item != null ? !item.equals(that.item) : that.item != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (value2 != null ? !value2.equals(that.value2) : that.value2 != null) return false;
        if (value3 != null ? !value3.equals(that.value3) : that.value3 != null) return false;
        if (value4 != null ? !value4.equals(that.value4) : that.value4 != null) return false;
        return deleted != null ? deleted.equals(that.deleted) : that.deleted == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (value2 != null ? value2.hashCode() : 0);
        result = 31 * result + (value3 != null ? value3.hashCode() : 0);
        result = 31 * result + (value4 != null ? value4.hashCode() : 0);
        result = 31 * result + (deleted != null ? deleted.hashCode() : 0);
        return result;
    }

    public static Creator<StoreObject> CREATOR = new Creator<StoreObject>() {
        @Override
        public StoreObject createFromParcel(Parcel source) {
            return new StoreObject(source);
        }

        @Override
        public StoreObject[] newArray(int size) {
            return new StoreObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(type);
        dest.writeString(item);
        dest.writeString(value);
        dest.writeString(value2);
        dest.writeString(value3);
        dest.writeString(value4);
        dest.writeLong(deleted);
    }

    @Override
    public String toString() {
        return "StoreObject{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", item='" + item + '\'' +
                ", value='" + value + '\'' +
                ", value2='" + value2 + '\'' +
                ", value3='" + value3 + '\'' +
                ", value4='" + value4 + '\'' +
                ", deleted=" + deleted +
                '}';
    }
}
