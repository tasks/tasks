package org.tasks.reminders;

import org.tasks.time.DateTime;

public class SnoozeOption {
    private final int resId;
    private final DateTime dateTime;

    public SnoozeOption(int resId, DateTime dateTime) {
        this.resId = resId;
        this.dateTime = dateTime;
    }

    public int getResId() {
        return resId;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    @Override
    public String toString() {
        return "SnoozeOption{" +
                "resId=" + resId +
                ", dateTime=" + dateTime +
                '}';
    }
}