package org.tasks.location;

import org.tasks.data.Place;

public class PlaceSearchResult {

  private final String id;
  private final String name;
  private final String address;
  private final Place place;

  @SuppressWarnings("unused")
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PlaceSearchResult that = (PlaceSearchResult) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }
    if (address != null ? !address.equals(that.address) : that.address != null) {
      return false;
    }
    return place != null ? place.equals(that.place) : that.place == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (address != null ? address.hashCode() : 0);
    result = 31 * result + (place != null ? place.hashCode() : 0);
    return result;
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
