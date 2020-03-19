package org.tasks.data;

import androidx.room.Embedded;

public class MergedGeofence {
  @Embedded Place place;
  boolean arrival;
  boolean departure;
  int radius;

  public Place getPlace() {
    return place;
  }

  public String getUid() {
    return place.getUid();
  }

  public double getLatitude() {
    return place.getLatitude();
  }

  public double getLongitude() {
    return place.getLongitude();
  }

  public boolean isArrival() {
    return arrival;
  }

  public boolean isDeparture() {
    return departure;
  }

  public int getRadius() {
    return radius;
  }

  @Override
  public String toString() {
    return "MergedGeofence{"
        + "place="
        + place.getDisplayName()
        + ", arrival="
        + arrival
        + ", departure="
        + departure
        + ", radius="
        + radius
                + '}';
  }
}
