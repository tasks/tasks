package org.tasks.calendars;

public class AndroidCalendarEvent {

    private long id;
    private final String title;
    private final long start;
    private final long end;

    public AndroidCalendarEvent(long id, String title, long start, long end) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
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

    @Override
    public String toString() {
        return "AndroidCalendarEvent{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
