package org.tasks.calendars;

import java.util.List;

public class AndroidCalendarEvent {

    private final long id;
    private final String title;
    private final long start;
    private final long end;
    private final List<AndroidCalendarEventAttendee> attendees;

    public AndroidCalendarEvent(long id, String title, long start, long end, List<AndroidCalendarEventAttendee> attendees) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.attendees = attendees;
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

    public List<AndroidCalendarEventAttendee> getAttendees() {
        return attendees;
    }

    @Override
    public String toString() {
        return "AndroidCalendarEvent{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", attendees=" + attendees +
                '}';
    }
}
