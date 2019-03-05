package org.tasks.data;

import androidx.room.Embedded;

public class PlaceUsage {
  @Embedded public Place place;
  public int count;

  public Place getPlace() {
    return place;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PlaceUsage that = (PlaceUsage) o;

    if (count != that.count) {
      return false;
    }
    return place != null ? place.equals(that.place) : that.place == null;
  }

  @Override
  public int hashCode() {
    int result = place != null ? place.hashCode() : 0;
    result = 31 * result + count;
    return result;
  }

  @Override
  public String toString() {
    return "PlaceUsage{" + "place=" + place + ", count=" + count + '}';
  }
}
