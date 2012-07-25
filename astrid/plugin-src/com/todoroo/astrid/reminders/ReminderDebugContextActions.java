/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import java.util.Date;

import android.widget.Toast;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.TaskContextActionExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.ReminderService.AlarmScheduler;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class ReminderDebugContextActions {

    public static class WhenReminder implements TaskContextActionExposer {

        @Override
        public Object getLabel(Task task) {
            if (Constants.DEBUG)
                return "when alarm?";
            return null;
        }

        public void invoke(Task task) {
            AlarmScheduler original = ReminderService.getInstance().getScheduler();
            ReminderService.getInstance().setScheduler(new AlarmScheduler() {
                @Override
                public void createAlarm(Task theTask, long time, int type) {
                    if(time == 0 || time == Long.MAX_VALUE)
                        return;

                    Toast.makeText(ContextManager.getContext(), "Scheduled Alarm: " +
                            new Date(time), Toast.LENGTH_LONG).show();
                    ReminderService.getInstance().setScheduler(null);
                }
            });
            ReminderService.getInstance().scheduleAlarm(task);
            if(ReminderService.getInstance().getScheduler() != null)
                Toast.makeText(ContextManager.getContext(), "No alarms", Toast.LENGTH_LONG).show();
            ReminderService.getInstance().setScheduler(original);
        }
    }

    public static class MakeNotification implements TaskContextActionExposer {

        public Object getLabel(Task task) {
            if (Constants.DEBUG)
                return "when alarm?"; //$NON-NLS-1$
            return null;
        }

        public void invoke(Task task) {
            new Notifications().showTaskNotification(task.getId(),
                        ReminderService.TYPE_SNOOZE, "test reminder");
        }
    }

}
