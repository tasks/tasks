package com.todoroo.astrid.reminders;

import com.todoroo.astrid.api.TaskContextActionExposer;
import com.todoroo.astrid.data.Task;

public class MakeNotification implements TaskContextActionExposer {

    @Override
    public void invoke(Task task) {
        new Notifications().showTaskNotification(task.getId(),
                ReminderService.TYPE_SNOOZE, "test reminder");
    }
}
