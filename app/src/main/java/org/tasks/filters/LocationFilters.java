package org.tasks.filters;

import androidx.room.Embedded;
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
    return "LocationFilters{" + "place=" + place + ", count=" + count + '}';
  }
}
