package org.tasks.reminders;

import org.tasks.time.DateTime;

public interface SnoozeCallback {

    void snoozeForTime(DateTime time);

    void pickDateTime();

}
