package com.todoroo.astrid.timers;

import com.todoroo.astrid.data.Task;

public interface TimerActionListener {
    void timerStopped(Task task);
    void timerStarted(Task task);
}
