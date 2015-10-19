package com.todoroo.astrid.gcal;

public class AndroidCalendar {
    private final String id;
    private final String name;

    public static final AndroidCalendar NONE = new AndroidCalendar("-1", "NONE");

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
}
