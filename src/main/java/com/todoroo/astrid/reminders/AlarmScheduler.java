package com.todoroo.astrid.reminders;

import com.todoroo.astrid.data.Task;

public interface AlarmScheduler {
    void createAlarm(Task task, long time, int type);

    void clear();
}
