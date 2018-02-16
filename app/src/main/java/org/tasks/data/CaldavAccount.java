package org.tasks.data;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

import static com.todoroo.astrid.data.Task.NO_UUID;

@Entity(tableName = "caldav_account")
public final class CaldavAccount implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private long id;

    @ColumnInfo(name = "uuid")
    private String uuid = NO_UUID;

    @ColumnInfo(name = "name")
    private String name = "";

    @ColumnInfo(name = "color")
    private int color = -1;

    @ColumnInfo(name = "deleted")
    private long deleted;

    @ColumnInfo(name = "ctag")
    private String ctag;

    public CaldavAccount() {

    }

    @Ignore
    public CaldavAccount(Parcel source) {
        id = source.readLong();
        uuid = source.readString();
        name = source.readString();
        color = source.readInt();
        deleted = source.readLong();
        ctag = source.readString();
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

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public String getCtag() {
        return ctag;
    }

    public void setCtag(String ctag) {
        this.ctag = ctag;
    }

    public boolean isDeleted() {
        return deleted > 0;
    }

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
        dest.writeLong(deleted);
        dest.writeString(ctag);
    }

    @Override
    public String toString() {
        return "CaldavAccount{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", color=" + color +
                ", deleted=" + deleted +
                ", ctag='" + ctag + '\'' +
                '}';
    }
}
