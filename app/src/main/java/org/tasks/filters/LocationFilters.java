package org.tasks.filters;

import androidx.room.Embedded;
import java.util.Objects;
import org.tasks.data.Place;

public class LocationFilters {
  @Embedded public Place place;
  public int count;

  PlaceFilter toLocationFilter() {
    PlaceFilter filter = new PlaceFilter(place);
    filter.count = count;
    return filter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LocationFilters)) {
      return false;
    }
    LocationFilters that = (LocationFilters) o;
    return count == that.count && Objects.equals(place, that.place);
  }

  @Override
  public int hashCode() {
    return Objects.hash(place, count);
  }

  @Override
  public String toString() {
    return "LocationFilters{" + "place=" + place + ", count=" + count + '}';
  }
}
