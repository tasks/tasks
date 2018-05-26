package org.tasks.calendars;

public class AndroidCalendarEventAttendee {

  private final String name;
  private final String email;

  public AndroidCalendarEventAttendee(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String toString() {
    return "AndroidCalendarEventAttendee{"
        + "name='"
        + name
        + '\''
        + ", email='"
        + email
        + '\''
        + '}';
  }
}
