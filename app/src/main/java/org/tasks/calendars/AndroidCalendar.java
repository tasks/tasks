package org.tasks.calendars;

public class AndroidCalendar {

  private final String id;
  private final String name;
  private final int color;

  public AndroidCalendar(String id, String name, int color) {
    this.id = id;
    this.name = name;
    this.color = color;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getColor() {
    return color;
  }

  @Override
  public String toString() {
    return "AndroidCalendar{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", color="
        + color
        + '}';
  }
}
