package org.tasks.location;

public class PlaceSearchResult {

  private final String id;
  private final String name;
  private final String address;
  private final Object tag;

  PlaceSearchResult(String id, String name, String address) {
    this(id, name, address, null);
  }

  PlaceSearchResult(String id, String name, String address, Object tag) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.tag = tag;
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

  public Object getTag() {
    return tag;
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
    return tag != null ? tag.equals(that.tag) : that.tag == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (address != null ? address.hashCode() : 0);
    result = 31 * result + (tag != null ? tag.hashCode() : 0);
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
        + ", tag="
        + tag
        + '}';
  }
}
