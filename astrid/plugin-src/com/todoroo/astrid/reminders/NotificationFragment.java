/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatControlSet;
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
public class NotificationFragment extends TaskListFragment {

    // --- constants

    /** task id from notification */
    public static final String TOKEN_ID = "id"; //$NON-NLS-1$

    // --- implementation

    private long taskId;

    /* (non-Javadoc)
     * @see com.todoroo.astrid.activity.TaskListActivity#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    protected void onTaskCompleted(Task item) {
        StatisticsService.reportEvent(StatisticsConstants.TASK_COMPLETED_NOTIFICATION);
    }

    @Override
    protected void initializeData() {
        displayNotificationPopup();
        super.initializeData();
    }

    /**
     * Set up the UI for this activity
     */
    private void displayNotificationPopup() {
        // hide quick add
        getView().findViewById(R.id.taskListFooter).setVisibility(View.GONE);

        String title = extras.getString(Notifications.EXTRAS_TEXT);
        taskId = extras.getLong(TOKEN_ID);
        new ReminderDialog((AstridActivity) getActivity(), taskId, title).show();
    }

    public static class SnoozeDialog extends FrameLayout implements DialogInterface.OnClickListener {

        LinearLayout snoozePicker;
        NumberPicker snoozeValue;
        Spinner snoozeUnits;
        SnoozeCallback snoozeCallback;

        public SnoozeDialog(Activity activity, SnoozeCallback callback) {
            super(activity);
            this.snoozeCallback = callback;

            LayoutInflater mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mInflater.inflate(R.layout.snooze_dialog, this, true);

            snoozePicker = (LinearLayout) findViewById(R.id.snoozePicker);
            snoozeValue = (NumberPicker) findViewById(R.id.numberPicker);
            snoozeUnits = (Spinner) findViewById(R.id.numberUnits);

            snoozeValue.setIncrementBy(1);
            snoozeValue.setRange(1, 99);
            snoozeUnits.setSelection(RepeatControlSet.INTERVAL_HOURS);
            snoozeUnits.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    AndroidUtilities.hideSoftInputForViews(getContext(), snoozePicker);
                    return false;
                }
            });
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

            snoozeCallback.snoozeForTime(time);
        }

    }

    /**
     * Snooze and re-trigger this alarm
     */
    public static void snooze(Activity activity, OnTimeSetListener onTimeSet, SnoozeCallback snoozeCallback) {
        if(Preferences.getBoolean(R.string.p_rmd_snooze_dialog, false)) {
            Date now = new Date();
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
