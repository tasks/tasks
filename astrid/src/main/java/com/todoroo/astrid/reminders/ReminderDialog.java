/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.app.Dialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.R;

import java.util.Date;

/**
 * A dialog that shows your task reminder
 *
 * @author sbosley
 *
 */
public class ReminderDialog extends Dialog {

    private static final int MAX_FACES = 4;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserDao userDao;

    public ReminderDialog(final AstridActivity activity, final long taskId,
            String title) {
        super(activity, R.style.ReminderDialog);
        DependencyInjectionService.getInstance().inject(this);
        final SnoozeCallback dialogSnooze = new SnoozeCallback() {
            @Override
            public void snoozeForTime(long time) {
                Task task = new Task();
                task.setId(taskId);
                task.setValue(Task.REMINDER_SNOOZE, time);
                PluginServices.getTaskService().save(task);
                dismiss();
            }
        };
        final OnTimeSetListener onTimeSet = new OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hours, int minutes) {
                Date alarmTime = new Date();
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
                NotificationFragment.snooze(activity, onTimeSet, dialogSnooze);
            }
        });

        findViewById(R.id.reminder_complete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Task task = taskService.fetchById(taskId, Task.ID, Task.REMINDER_LAST, Task.SOCIAL_REMINDER);
                if (task != null) {
                    taskService.setComplete(task, true);
                }
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
