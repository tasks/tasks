/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.Dialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;

/**
 * A dialog that shows your task reminder
 *
 * @author sbosley
 *
 */
public class ReminderDialog extends Dialog {

    public ReminderDialog(final AstridActivity activity, final long taskId,
            String title) {
        super(activity, R.style.ReminderDialog);
        final SnoozeCallback dialogSnooze = new SnoozeCallback() {
            @Override
            public void snoozeForTime(long time) {
                Task task = new Task();
                task.setId(taskId);
                task.setValue(Task.REMINDER_SNOOZE, time);
                PluginServices.getTaskService().save(task);
                dismiss();
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

        if (Preferences.getBoolean(R.string.p_rmd_nagging, true)) {
            setContentView(R.layout.astrid_reminder_view);
            ((TextView) findViewById(R.id.reminder_message)).setText(
                    Notifications.getRandomReminder(activity.getResources().getStringArray(R.array.reminder_responses)));
        } else {
            setContentView(R.layout.astrid_reminder_view_portrait);
            title = activity.getString(R.string.rmd_NoA_dlg_title) + " " + title; //$NON-NLS-1$
            removeSpeechBubble();
        }


        // set up listeners
        findViewById(R.id.dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dismiss();
            }
        });

        findViewById(R.id.reminder_snooze).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                NotificationFragment.snooze(activity, onTimeSet, dialogSnooze);
            }
        });

        findViewById(R.id.reminder_complete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Task task = new Task();
                task.setId(taskId);
                PluginServices.getTaskService().setComplete(task, true);
                activity.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                Toast.makeText(activity,
                        R.string.rmd_NoA_completed_toast,
                        Toast.LENGTH_LONG).show();
                dismiss();
            }
        });

        findViewById(R.id.reminder_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                activity.onTaskListItemClicked(taskId);
            }
        });

        ((TextView) findViewById(R.id.reminder_title)).setText(title);

        setOwnerActivity(activity);
    }


    private void removeSpeechBubble() {
        LinearLayout container = (LinearLayout) findViewById(R.id.speech_bubble_container);
        container.setVisibility(View.GONE);
    }
}
