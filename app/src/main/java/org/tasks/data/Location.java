package org.tasks.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Ignore;
import java.io.Serializable;
import java.util.Objects;

public class Location implements Serializable, Parcelable {

  public static final Parcelable.Creator<Location> CREATOR =
      new Parcelable.Creator<Location>() {
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
  private Location(Parcel in) {
    geofence = in.readParcelable(Geofence.class.getClassLoader());
    place = in.readParcelable(Place.class.getClassLoader());
  }

  public long getTask() {
    return geofence.getTask();
  }

  public double getLatitude() {
    return place.getLatitude();
  }

  public double getLongitude() {
    return place.getLongitude();
  }

  public int getRadius() {
    return geofence.getRadius();
  }

  public String getPhone() {
    return place.getPhone();
  }

  public String getUrl() {
    return place.getUrl();
  }

  public boolean isArrival() {
    return geofence.isArrival();
  }

  public boolean isDeparture() {
    return geofence.isDeparture();
  }

  public String getDisplayName() {
    return place.getDisplayName();
  }

  public String getDisplayAddress() {
    return place.getDisplayAddress();
  }

  public void open(@Nullable Context context) {
    place.open(context);
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
    if (!(o instanceof Location)) {
      return false;
    }
    Location location = (Location) o;
    return Objects.equals(geofence, location.geofence) && Objects.equals(place, location.place);
  }

  @Override
  public int hashCode() {
    return Objects.hash(geofence, place);
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

  public Place getPlace() {
    return place;
  }
}
