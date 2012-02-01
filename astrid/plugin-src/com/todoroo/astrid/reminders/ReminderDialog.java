package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.Dialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.TimePicker;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;


public class ReminderDialog {

    public static void showReminderDialog(final AstridActivity activity, final long taskId, String title) {
        final Dialog d = new Dialog(activity, R.style.ReminderDialog);
        final SnoozeCallback dialogSnooze = new SnoozeCallback() {
            @Override
            public void snoozeForTime(long time) {
                Task task = new Task();
                task.setId(taskId);
                task.setValue(Task.REMINDER_SNOOZE, time);
                PluginServices.getTaskService().save(task);
                d.dismiss();
                StatisticsService.reportEvent(StatisticsConstants.TASK_SNOOZE);
            }
        };
        final OnTimeSetListener onTimeSet = new OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hours, int minutes) {
                Date alarmTime = new Date();
                alarmTime.setHours(hours);
                alarmTime.setMinutes(minutes);
                if(alarmTime.getTime() < DateUtilities.now())
                    alarmTime.setDate(alarmTime.getDate() + 1);
                dialogSnooze.snoozeForTime(alarmTime.getTime());
            }
        };
        d.setContentView(R.layout.astrid_reminder_view);

        // set up listeners
        d.findViewById(R.id.dismiss).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                d.dismiss();
            }
        });

        d.findViewById(R.id.reminder_snooze).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                NotificationFragment.snooze(activity, onTimeSet, dialogSnooze);
            }
        });

        d.findViewById(R.id.reminder_complete).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Task task = new Task();
                task.setId(taskId);
                PluginServices.getTaskService().setComplete(task, true);
                d.dismiss();
            }
        });

        d.findViewById(R.id.reminder_edit).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                activity.onTaskListItemClicked(taskId);
            }
        });

        ((TextView) d.findViewById(R.id.reminder_title)).setText(title);
        ((TextView) d.findViewById(R.id.reminder_message)).setText(
                Notifications.getRandomReminder(activity.getResources().getStringArray(R.array.reminder_responses)));

        d.setOwnerActivity(activity);
        d.show();
    }

}
