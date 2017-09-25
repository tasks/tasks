package org.tasks.calendars;

public class AndroidCalendar {
    private final String id;
    private final String name;

    public AndroidCalendar(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AndroidCalendar{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
