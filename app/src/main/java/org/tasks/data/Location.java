package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;
import org.tasks.backup.XmlReader;

@Entity(tableName = "locations")
public class Location implements Serializable, Parcelable {

  public static final Parcelable.Creator<Location> CREATOR =
      new Parcelable.Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel source) {
          return new Location(source);
        }

        @Override
        public Location[] newArray(int size) {
          return new Location[size];
        }
      };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient long id;

  @ColumnInfo(name = "task")
  private transient long task;

  @ColumnInfo(name = "name")
  private String name;

  @ColumnInfo(name = "latitude")
  private double latitude;

  @ColumnInfo(name = "longitude")
  private double longitude;

  @ColumnInfo(name = "radius")
  private int radius;

  public Location() {}

  @Ignore
  public Location(Parcel parcel) {
    id = parcel.readLong();
    task = parcel.readLong();
    name = parcel.readString();
    latitude = parcel.readDouble();
    longitude = parcel.readDouble();
    radius = parcel.readInt();
  }

  @Ignore
  public Location(XmlReader xml) {
    xml.readString("name", this::setName);
    xml.readDouble("latitude", this::setLatitude);
    xml.readDouble("longitude", this::setLongitude);
    xml.readInteger("radius", this::setRadius);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getTask() {
    return task;
  }

  public void setTask(long task) {
    this.task = task;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public int getRadius() {
    return radius;
  }

  public void setRadius(int radius) {
    this.radius = radius;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Location location = (Location) o;

    if (id != location.id) {
      return false;
    }
    if (task != location.task) {
      return false;
    }
    if (Double.compare(location.latitude, latitude) != 0) {
      return false;
    }
    if (Double.compare(location.longitude, longitude) != 0) {
      return false;
    }
    if (radius != location.radius) {
      return false;
    }
    return name != null ? name.equals(location.name) : location.name == null;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (task ^ (task >>> 32));
    result = 31 * result + (name != null ? name.hashCode() : 0);
    temp = Double.doubleToLongBits(latitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(longitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + radius;
    return result;
  }

  @Override
  public String toString() {
    return "Location{"
        + "id="
        + id
        + ", task="
        + task
        + ", name='"
        + name
        + '\''
        + ", latitude="
        + latitude
        + ", longitude="
        + longitude
        + ", radius="
        + radius
        + '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeLong(id);
    out.writeLong(task);
    out.writeString(name);
    out.writeDouble(latitude);
    out.writeDouble(longitude);
    out.writeInt(radius);
  }
}
