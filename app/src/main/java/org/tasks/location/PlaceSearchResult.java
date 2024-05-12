package org.tasks.location;

import java.util.Objects;
import org.tasks.data.entity.Place;

public class PlaceSearchResult {

  private final String id;
  private final String name;
  private final String address;
  private final Place place;

  PlaceSearchResult(String id, String name, String address) {
    this(id, name, address, null);
  }

  PlaceSearchResult(String id, String name, String address, Place place) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.place = place;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public Place getPlace() {
    return place;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PlaceSearchResult)) {
      return false;
    }
    PlaceSearchResult that = (PlaceSearchResult) o;
    return Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(address, that.address)
        && Objects.equals(place, that.place);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, address, place);
  }

  @Override
  public String toString() {
    return "PlaceSearchResult{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", address='"
        + address
        + '\''
        + ", place="
        + place
        + '}';
  }
}
