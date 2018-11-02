package org.tasks.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.regex.Pattern;
import org.tasks.backup.XmlReader;

@Entity(tableName = "locations")
public class Location implements Serializable, Parcelable {

  private static final Pattern COORDS =
      Pattern.compile("^\\d+°\\d+'\\d+\\.\\d+\"[NS] \\d+°\\d+'\\d+\\.\\d+\"[EW]$");

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

  @ColumnInfo(name = "address")
  private String address;

  @ColumnInfo(name = "phone")
  private String phone;

  @ColumnInfo(name = "url")
  private String url;

  @ColumnInfo(name = "latitude")
  private double latitude;

  @ColumnInfo(name = "longitude")
  private double longitude;

  @ColumnInfo(name = "radius")
  private int radius;

  @ColumnInfo(name = "arrival")
  private boolean arrival;

  @ColumnInfo(name = "departure")
  private boolean departure;

  public Location() {}

  @Ignore
  public Location(Location o) {
    id = o.id;
    task = o.task;
    name = o.name;
    address = o.address;
    phone = o.phone;
    url = o.url;
    latitude = o.latitude;
    longitude = o.longitude;
    radius = o.radius;
    arrival = o.arrival;
    departure = o.departure;
  }

  @Ignore
  public Location(Parcel parcel) {
    id = parcel.readLong();
    task = parcel.readLong();
    name = parcel.readString();
    address = parcel.readString();
    phone = parcel.readString();
    url = parcel.readString();
    latitude = parcel.readDouble();
    longitude = parcel.readDouble();
    radius = parcel.readInt();
    arrival = parcel.readInt() == 1;
    departure = parcel.readInt() == 1;
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

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isArrival() {
    return arrival;
  }

  public void setArrival(boolean arrival) {
    this.arrival = arrival;
  }

  public boolean isDeparture() {
    return departure;
  }

  public void setDeparture(boolean departure) {
    this.departure = departure;
  }

  public String getDisplayName() {
    if (Strings.isNullOrEmpty(address)) {
      return name;
    }
    if (COORDS.matcher(name).matches()) {
      return address;
    }
    if (address.startsWith(name)) {
      return address;
    }
    return name;
  }

  public String getGeoUri() {
    return String.format("geo:%s,%s?q=%s", latitude, longitude,
        Uri.encode(Strings.isNullOrEmpty(address) ? name : address));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Location)) {
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
    if (arrival != location.arrival) {
      return false;
    }
    if (departure != location.departure) {
      return false;
    }
    if (name != null ? !name.equals(location.name) : location.name != null) {
      return false;
    }
    if (address != null ? !address.equals(location.address) : location.address != null) {
      return false;
    }
    if (phone != null ? !phone.equals(location.phone) : location.phone != null) {
      return false;
    }
    return url != null ? url.equals(location.url) : location.url == null;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (task ^ (task >>> 32));
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (address != null ? address.hashCode() : 0);
    result = 31 * result + (phone != null ? phone.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    temp = Double.doubleToLongBits(latitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(longitude);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + radius;
    result = 31 * result + (arrival ? 1 : 0);
    result = 31 * result + (departure ? 1 : 0);
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
        + ", address='"
        + address
        + '\''
        + ", phone='"
        + phone
        + '\''
        + ", url='"
        + url
        + '\''
        + ", latitude="
        + latitude
        + ", longitude="
        + longitude
        + ", radius="
        + radius
        + ", arrival="
        + arrival
        + ", departure="
        + departure
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
    out.writeString(address);
    out.writeString(phone);
    out.writeString(url);
    out.writeDouble(latitude);
    out.writeDouble(longitude);
    out.writeInt(radius);
    out.writeInt(arrival ? 1 : 0);
    out.writeInt(departure ? 1 : 0);
  }
}
