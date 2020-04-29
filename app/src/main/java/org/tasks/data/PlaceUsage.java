package org.tasks.data;

import androidx.room.Embedded;
import java.util.Objects;

public class PlaceUsage {
  @Embedded public Place place;
  public int count;

  public Place getPlace() {
    return place;
  }

  public int getColor() {
    return place.getColor();
  }

  public int getIcon() {
    return place.getIcon();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PlaceUsage)) {
      return false;
    }
    PlaceUsage that = (PlaceUsage) o;
    return count == that.count && Objects.equals(place, that.place);
  }

  @Override
  public int hashCode() {
    return Objects.hash(place, count);
  }

  @Override
  public String toString() {
    return "PlaceUsage{" + "place=" + place + ", count=" + count + '}';
  }
}
