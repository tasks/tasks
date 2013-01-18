/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import java.util.Date;
import java.util.LinkedHashSet;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.ui.DateAndTimeDialog;
import com.todoroo.astrid.ui.DateAndTimeDialog.DateAndTimeDialogListener;
import com.todoroo.astrid.ui.DateAndTimePicker;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class AlarmControlSet extends TaskEditControlSet {

    // --- instance variables

    private LinearLayout alertsContainer;
    private DateAndTimeDialog pickerDialog;

    public AlarmControlSet(Activity activity, int layout) {
        super(activity, layout);
    }

    @Override
    protected void readFromTaskOnInitialize() {
        alertsContainer.removeAllViews();
        TodorooCursor<Metadata> cursor = AlarmService.getInstance().getAlarms(model.getId());
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                addAlarm(new Date(cursor.get(AlarmFields.TIME)));
        } finally {
            cursor.close();
        }
    }

    @Override
    protected void afterInflate() {
        this.alertsContainer = (LinearLayout) getView().findViewById(R.id.alert_container);
        View.OnClickListener addAlarmListener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                addAlarm(new Date());
            }
        };
        getView().findViewById(R.id.alarms_add).setOnClickListener(addAlarmListener);

        pickerDialog = new DateAndTimeDialog(activity, 0);
    }

    @Override
    public String writeToModel(Task task) {
        if (initialized && pickerDialog != null)
            pickerDialog.dismiss();
        return super.writeToModel(task);
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        LinkedHashSet<Long> alarms = new LinkedHashSet<Long>();
        for(int i = 0; i < alertsContainer.getChildCount(); i++) {
            Long dateValue = (Long) alertsContainer.getChildAt(i).getTag();
            if(dateValue == null)
                continue;
            alarms.add(dateValue);
        }

        if(AlarmService.getInstance().synchronizeAlarms(task.getId(), alarms))
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());

        return null;
    }

    private boolean addAlarm(Date alert) {
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
                                Date d = new Date(date);
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
            public void onClick(View v) {
                alertsContainer.removeView(alertItem);
            }
        });

        return true;
    }
}
