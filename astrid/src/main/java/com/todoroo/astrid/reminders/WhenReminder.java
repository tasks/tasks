package com.todoroo.astrid.reminders;

import android.widget.Toast;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.TaskContextActionExposer;
import com.todoroo.astrid.data.Task;

import java.util.Date;

public class WhenReminder implements TaskContextActionExposer {

    @Override
    public void invoke(Task task) {
        ReminderService.AlarmScheduler original = ReminderService.getInstance().getScheduler();
        ReminderService.getInstance().setScheduler(new ReminderService.AlarmScheduler() {
            @Override
            public void createAlarm(Task theTask, long time, int type) {
                if(time == 0 || time == Long.MAX_VALUE) {
                    return;
                }

                Toast.makeText(ContextManager.getContext(), "Scheduled Alarm: " +
                        new Date(time), Toast.LENGTH_LONG).show();
                ReminderService.getInstance().setScheduler(null);
            }
        });
        ReminderService.getInstance().scheduleAlarm(task);
        if(ReminderService.getInstance().getScheduler() != null) {
            Toast.makeText(ContextManager.getContext(), "No alarms", Toast.LENGTH_LONG).show();
        }
        ReminderService.getInstance().setScheduler(original);
    }
}
