package org.tasks.reminders;

import org.tasks.time.DateTime;

interface SnoozeCallback {

  void snoozeForTime(DateTime time);

  void pickDateTime();
}
