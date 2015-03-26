/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.joda.time.DateTime;
import org.tasks.R;
import org.tasks.dialogs.DateAndTimePickerDialog;

import java.util.Date;
import java.util.LinkedHashSet;

import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.date.DateTimeUtils.newDateTime;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class AlarmControlSet extends TaskEditControlSetBase {

    private final AlarmService alarmService;
    private TaskEditFragment taskEditFragment;

    private LinearLayout alertsContainer;

    public AlarmControlSet(AlarmService alarmService, TaskEditFragment taskEditFragment) {
        super(taskEditFragment.getActivity(), R.layout.control_set_alarms);
        this.alarmService = alarmService;
        this.taskEditFragment = taskEditFragment;
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
                DateAndTimePickerDialog.dateAndTimePickerDialog(taskEditFragment.getFragmentManager(), taskEditFragment.getActivity(), newDateTime((Long) alertItem.getTag()), new DateAndTimePickerDialog.OnDateTimePicked() {
                    @Override
                    public void onDateTimePicked(DateTime dateTime) {
                        v.setTag(dateTime.getMillis());
                        TextView label = (TextView) v.findViewById(R.id.alarm_string);
                        label.setText(getDisplayString(activity, dateTime.getMillis()));
                    }
                }, new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                    }
                });
            }
        });

        alertItem.setTag(alert.getTime());
        TextView display = (TextView) alertItem.findViewById(R.id.alarm_string);
        display.setText(getDisplayString(activity, alert.getTime()));

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)alertItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertsContainer.removeView(alertItem);
            }
        });
    }

    public static String getDisplayString(Context context, long forDate) {
        DateTime dateTime = newDateTime(forDate);
        Date d = dateTime.toDate();
        return (dateTime.getYear() == newDateTime().getYear()
                ? DateUtilities.getDateStringHideYear(d)
                : DateUtilities.getDateString(d)) +
                ", " + DateUtilities.getTimeString(context, d);
    }
}
