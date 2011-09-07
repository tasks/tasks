/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.ui.NumberPicker;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class NotificationActivity extends TaskListActivity implements OnTimeSetListener {

    // --- constants

    /** task id from notification */
    public static final String TOKEN_ID = "id"; //$NON-NLS-1$

    // --- implementation

    private long taskId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        StartupService.bypassInitialization();

        super.onCreate(savedInstanceState);
        displayNotificationPopup();
    }

    @Override
    protected void onTaskCompleted(Task item) {
        StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_NOTIFICATION);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        populateFilter(intent);
        super.onNewIntent(intent);
    }

    private void populateFilter(Intent intent) {
        taskId = intent.getLongExtra(TOKEN_ID, -1);
        if(taskId == -1)
            return;

        Filter itemFilter = new Filter(getString(R.string.rmd_NoA_filter),
                getString(R.string.rmd_NoA_filter),
                new QueryTemplate().where(TaskCriteria.byId(taskId)),
                null);
        intent.putExtra(TaskListActivity.TOKEN_FILTER, itemFilter);
    }

    /**
     * Set up the UI for this activity
     */
    private void displayNotificationPopup() {
        // hide quick add
        findViewById(R.id.taskListFooter).setVisibility(View.GONE);

        // instantiate reminder window
        ViewGroup parent = (ViewGroup) findViewById(R.id.taskListParent);
        getLayoutInflater().inflate(R.layout.notification_control, parent, true);

        String reminder = Notifications.getRandomReminder(getResources().getStringArray(R.array.reminder_responses));

        if(Preferences.getBoolean(R.string.p_rmd_nagging, true))
            ((TextView)findViewById(R.id.reminderLabel)).setText(reminder);
        else {
            findViewById(R.id.reminderLabel).setVisibility(View.GONE);
            findViewById(R.id.astridIcon).setVisibility(View.GONE);
        }

        // set up listeners
        ((Button)findViewById(R.id.goAway)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });

        ((Button)findViewById(R.id.snooze)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                snooze();
            }
        });

        ((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Task task = new Task();
                task.setId(taskId);
                PluginServices.getTaskService().setComplete(task, true);
                finish();
            }
        });
    }

    public static class SnoozeDialog extends FrameLayout implements DialogInterface.OnClickListener {

        LinearLayout snoozePicker;
        NumberPicker snoozeValue;
        Spinner snoozeUnits;
        NotificationActivity parent;

        public SnoozeDialog(NotificationActivity parent) {
            super(parent);
            this.parent = parent;

            LayoutInflater mInflater = (LayoutInflater) parent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mInflater.inflate(R.layout.snooze_dialog, this, true);

            snoozePicker = (LinearLayout) findViewById(R.id.snoozePicker);
            snoozeValue = (NumberPicker) findViewById(R.id.numberPicker);
            snoozeUnits = (Spinner) findViewById(R.id.numberUnits);

            snoozeValue.setIncrementBy(1);
            snoozeValue.setRange(1, 99);
            snoozeUnits.setSelection(RepeatControlSet.INTERVAL_HOURS);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            long time = DateUtilities.now();
            int value = snoozeValue.getCurrent();
            switch(snoozeUnits.getSelectedItemPosition()) {
            case RepeatControlSet.INTERVAL_DAYS:
                time += value * DateUtilities.ONE_DAY;
                break;
            case RepeatControlSet.INTERVAL_HOURS:
                time += value * DateUtilities.ONE_HOUR;
                break;
            case RepeatControlSet.INTERVAL_MINUTES:
                time += value * DateUtilities.ONE_MINUTE;
                break;
            case RepeatControlSet.INTERVAL_WEEKS:
                time += value * 7 * DateUtilities.ONE_DAY;
                break;
            case RepeatControlSet.INTERVAL_MONTHS:
                time = DateUtilities.addCalendarMonthsToUnixtime(time, 1);
                break;
            case RepeatControlSet.INTERVAL_YEARS:
                time = DateUtilities.addCalendarMonthsToUnixtime(time, 12);
                break;
            }

            parent.snoozeTime(time);
        }

    }

    /**
     * Snooze and re-trigger this alarm
     */
    private void snooze() {
        if(Preferences.getBoolean(R.string.p_rmd_snooze_dialog, false)) {
            Date now = new Date();
            now.setHours(now.getHours() + 1);
            int hour = now.getHours();
            int minute = now.getMinutes();
            TimePickerDialog tpd = new TimePickerDialog(this, this, hour, minute,
                    DateUtilities.is24HourFormat(this));
            tpd.show();
            tpd.setOwnerActivity(this);
        } else {
            SnoozeDialog sd = new SnoozeDialog(this);
            new AlertDialog.Builder(this)
                .setTitle(R.string.rmd_NoA_snooze)
                .setView(sd)
                .setPositiveButton(android.R.string.ok, sd)
                .setNegativeButton(android.R.string.cancel, null)
                .show().setOwnerActivity(this);
        }
    }

    /** on time dialog return set */
    @Override
    public void onTimeSet(TimePicker picker, int hours, int minutes) {
        Date alarmTime = new Date();
        alarmTime.setHours(hours);
        alarmTime.setMinutes(minutes);
        if(alarmTime.getTime() < DateUtilities.now())
            alarmTime.setDate(alarmTime.getDate() + 1);
        snoozeTime(alarmTime.getTime());
    }

    public void snoozeTime(long time) {
        Task task = new Task();
        task.setId(taskId);
        task.setValue(Task.REMINDER_SNOOZE, time);
        PluginServices.getTaskService().save(task);
        finish();
        StatisticsService.reportEvent(StatisticsConstants.TASK_SNOOZE);
    }

}
