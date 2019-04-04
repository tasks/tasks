package org.tasks.data;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.Embedded;
import androidx.room.Ignore;
import java.io.Serializable;

public class Location implements Serializable, Parcelable {

  public static final Creator<Location> CREATOR =
      new Creator<Location>() {
        @Override
        public Location createFromParcel(Parcel in) {
          return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
          return new Location[size];
        }
      };

  @Embedded public Geofence geofence;
  @Embedded public Place place;

  public Location() {}

  @Ignore
  public Location(Geofence geofence, Place place) {
    this.geofence = geofence;
    this.place = place;
  }

  @Ignore
  protected Location(Parcel in) {
    geofence = in.readParcelable(Geofence.class.getClassLoader());
    place = in.readParcelable(Place.class.getClassLoader());
  }

  public long getId() {
    return geofence.getId();
  }

  public void setId(long id) {
    geofence.setId(id);
  }

  public long getTask() {
    return geofence.getTask();
  }

  public void setTask(long task) {
    geofence.setTask(task);
  }

  public String getName() {
    return place.getName();
  }

  public void setName(String name) {
    place.setName(name);
  }

  public double getLatitude() {
    return place.getLatitude();
  }

  public void setLatitude(double latitude) {
    place.setLatitude(latitude);
  }

  public double getLongitude() {
    return place.getLongitude();
  }

  public void setLongitude(double longitude) {
    place.setLongitude(longitude);
  }

  public int getRadius() {
    return geofence.getRadius();
  }

  public void setRadius(int radius) {
    geofence.setRadius(radius);
  }

  public String getAddress() {
    return place.getAddress();
  }

  public String getPhone() {
    return place.getPhone();
  }

  public void setPhone(String phone) {
    place.setPhone(phone);
  }

  public String getUrl() {
    return place.getUrl();
  }

  public void setUrl(String url) {
    place.setUrl(url);
  }

  public boolean isArrival() {
    return geofence.isArrival();
  }

  public void setArrival(boolean arrival) {
    geofence.setArrival(arrival);
  }

  public boolean isDeparture() {
    return geofence.isDeparture();
  }

  public void setDeparture(boolean departure) {
    geofence.setDeparture(departure);
  }

  public String getDisplayName() {
    return place.getDisplayName();
  }

  public String getDisplayAddress() {
    return place.getDisplayAddress();
  }

  public String getGeoUri() {
    return place.getGeoUri();
  }

  @Override
  public String toString() {
    return "Location{" + "geofence=" + geofence + ", place=" + place + '}';
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

    if (geofence != null ? !geofence.equals(location.geofence) : location.geofence != null) {
      return false;
    }
    return place != null ? place.equals(location.place) : location.place == null;
  }

  @Override
  public int hashCode() {
    int result = geofence != null ? geofence.hashCode() : 0;
    result = 31 * result + (place != null ? place.hashCode() : 0);
    return result;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(geofence, flags);
    dest.writeParcelable(place, flags);
  }
}
