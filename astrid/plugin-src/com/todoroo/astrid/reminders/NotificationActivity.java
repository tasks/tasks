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

import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.utility.Preferences;

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
        populateFilter(getIntent());
        super.onCreate(savedInstanceState);

        displayNotificationPopup();
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
    }

    /**
     * Snooze and re-trigger this alarm
     */
    private void snooze() {
        Date now = new Date();
        now.setHours(now.getHours() + 1);
        int hour = now.getHours();
        int minute = now.getMinutes();
        TimePickerDialog timePicker = new TimePickerDialog(this, this,
                hour, minute, DateUtilities.is24HourFormat(this));
        timePicker.show();
    }

    /** snooze timer set */
    @Override
    public void onTimeSet(TimePicker picker, int hours, int minutes) {
        Date alarmTime = new Date();
        alarmTime.setHours(hours);
        alarmTime.setMinutes(minutes);
        if(alarmTime.getTime() < DateUtilities.now())
            alarmTime.setDate(alarmTime.getDate() + 1);
        ReminderService.getInstance().scheduleSnoozeAlarm(taskId, alarmTime.getTime());
        finish();
    }


}
