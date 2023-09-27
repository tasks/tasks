package org.tasks.calendars;

public class AndroidCalendarEvent {

  private final long id;
  private final String title;
  private final long start;
  private final long end;
  private final int calendarId;

  public AndroidCalendarEvent(
      long id,
      String title,
      long start,
      long end,
      int calendarId) {
    this.id = id;
    this.title = title;
    this.start = start;
    this.end = end;
    this.calendarId = calendarId;
  }

  public long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  public int getCalendarId() {
    return calendarId;
  }

  @Override
  public String toString() {
    return "AndroidCalendarEvent{"
        + "id="
        + id
        + ", title='"
        + title
        + '\''
        + ", start="
        + start
        + ", end="
        + end
        + ", calendarId="
        + calendarId
        + '}';
  }
}
