/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.Date;

import static org.tasks.date.DateTimeUtils.newDate;

/**
 * A dialog that shows your task reminder
 *
 * @author sbosley
 *
 */
public class ReminderDialog extends Dialog {

    private final Preferences preferences;

    public ReminderDialog(Preferences preferences, final Broadcaster broadcaster, final TaskService taskService, final AstridActivity activity, final long taskId,
            String title) {
        super(activity, R.style.ReminderDialog);
        this.preferences = preferences;

        final SnoozeCallback dialogSnooze = new SnoozeCallback() {
            @Override
            public void snoozeForTime(long time) {
                Task task = new Task();
                task.setId(taskId);
                task.setReminderSnooze(time);
                taskService.save(task);
                dismiss();
            }
        };
        final OnTimeSetListener onTimeSet = new OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hours, int minutes) {
                Date alarmTime = newDate();
                alarmTime.setHours(hours);
                alarmTime.setMinutes(minutes);
                if(alarmTime.getTime() < DateUtilities.now()) {
                    alarmTime.setDate(alarmTime.getDate() + 1);
                }
                dialogSnooze.snoozeForTime(alarmTime.getTime());
            }
        };

        setContentView(R.layout.astrid_reminder_view_portrait);
        title = activity.getString(R.string.rmd_NoA_dlg_title) + " " + title; //$NON-NLS-1$
        removeSpeechBubble();

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
                snooze(activity, onTimeSet, dialogSnooze);
            }
        });

        findViewById(R.id.reminder_complete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                broadcaster.completeTask(taskId);
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

    /**
     * Snooze and re-trigger this alarm
     */
    private void snooze(Activity activity, OnTimeSetListener onTimeSet, SnoozeCallback snoozeCallback) {
        if(preferences.getBoolean(R.string.p_rmd_snooze_dialog, false)) {
            Date now = newDate();
            now.setHours(now.getHours() + 1);
            int hour = now.getHours();
            int minute = now.getMinutes();
            TimePickerDialog tpd = new TimePickerDialog(activity, onTimeSet, hour, minute,
                    DateUtilities.is24HourFormat(activity));
            tpd.show();
            tpd.setOwnerActivity(activity);
        } else {
            SnoozeDialog sd = new SnoozeDialog(activity, snoozeCallback);
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.rmd_NoA_snooze)
                    .setView(sd)
                    .setPositiveButton(android.R.string.ok, sd)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show().setOwnerActivity(activity);
        }
    }
}
