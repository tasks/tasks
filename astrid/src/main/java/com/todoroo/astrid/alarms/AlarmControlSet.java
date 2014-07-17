/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.ui.DateAndTimeDialog;
import com.todoroo.astrid.ui.DateAndTimeDialog.DateAndTimeDialogListener;
import com.todoroo.astrid.ui.DateAndTimePicker;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import java.util.Date;
import java.util.LinkedHashSet;

import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class AlarmControlSet extends TaskEditControlSet {

    private final ActivityPreferences preferences;
    private final AlarmService alarmService;

    private LinearLayout alertsContainer;
    private DateAndTimeDialog pickerDialog;

    public AlarmControlSet(ActivityPreferences preferences, AlarmService alarmService, Activity activity) {
        super(activity, R.layout.control_set_alarms);
        this.preferences = preferences;
        this.alarmService = alarmService;
    }

    @Override
    protected void readFromTaskOnInitialize() {
        alertsContainer.removeAllViews();
        alarmService.getAlarms(model.getId(), new Callback<Metadata>() {
            @Override
            public void apply(Metadata entry) {
                addAlarm(newDate(entry.getValue(AlarmFields.TIME)));
            }
        });
    }

    @Override
    protected void afterInflate() {
        this.alertsContainer = (LinearLayout) getView().findViewById(R.id.alert_container);
        View.OnClickListener addAlarmListener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                addAlarm(newDate());
            }
        };
        getView().findViewById(R.id.alarms_add).setOnClickListener(addAlarmListener);

        pickerDialog = new DateAndTimeDialog(preferences, activity, 0);
    }

    @Override
    public void writeToModel(Task task) {
        if (initialized && pickerDialog != null) {
            pickerDialog.dismiss();
        }
        super.writeToModel(task);
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        LinkedHashSet<Long> alarms = new LinkedHashSet<>();
        for(int i = 0; i < alertsContainer.getChildCount(); i++) {
            Long dateValue = (Long) alertsContainer.getChildAt(i).getTag();
            if(dateValue == null) {
                continue;
            }
            alarms.add(dateValue);
        }

        if(alarmService.synchronizeAlarms(task.getId(), alarms)) {
            task.setModificationDate(DateUtilities.now());
        }
    }

    private void addAlarm(Date alert) {
        final View alertItem = LayoutInflater.from(activity).inflate(R.layout.alarm_edit_row, null);
        alertsContainer.addView(alertItem);

        alertItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                pickerDialog.setSelectedDateAndTime((Long) alertItem.getTag());
                pickerDialog.setDateAndTimeDialogListener(new DateAndTimeDialogListener() {
                    @Override
                    public void onDateAndTimeSelected(long date) {
                        if (date > 0) {
                            if (!pickerDialog.hasTime()) {
                                Date d = newDate(date);
                                d.setHours(18);
                                d.setMinutes(0);
                                d.setSeconds(0);
                                date = d.getTime();
                            }
                            v.setTag(date);
                            TextView label = (TextView) v.findViewById(R.id.alarm_string);
                            label.setText(DateAndTimePicker.getDisplayString(activity, date));
                        }
                    }

                    @Override
                    public void onDateAndTimeCancelled() {
                        // Do nothing
                    }
                });
                pickerDialog.show();
            }
        });

        alertItem.setTag(alert.getTime());
        TextView display = (TextView) alertItem.findViewById(R.id.alarm_string);
        display.setText(DateAndTimePicker.getDisplayString(activity, alert.getTime()));

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)alertItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertsContainer.removeView(alertItem);
            }
        });
    }
}
