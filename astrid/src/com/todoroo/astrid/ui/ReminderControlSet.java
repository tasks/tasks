/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.alarms.AlarmControlSet;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.ThemeService;

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderControlSet extends PopupControlSet {
    private CheckBox during, after;
    private Spinner mode;
    private TextView modeDisplay;
    private LinearLayout remindersBody;
    private final List<View> extraViews;
    private final TextView label;
    private final ImageView image;

    private RandomReminderControlSet randomControlSet;
    private AlarmControlSet alarmControl;

    public ReminderControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, R.string.TEA_reminders_group_label);
        extraViews = new ArrayList<View>();
        label = (TextView) getDisplayView().findViewById(R.id.display_row_edit);

        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    public void addViewToBody(View v) {
        if (remindersBody != null)
            remindersBody.addView(v, 0);
        else
            extraViews.add(v);

    }

    public void setValue(int flags) {
        during.setChecked((flags & Task.NOTIFY_AT_DEADLINE) > 0);
        after.setChecked((flags &
                Task.NOTIFY_AFTER_DEADLINE) > 0);

        if((flags & Task.NOTIFY_MODE_NONSTOP) > 0)
            mode.setSelection(2);
        else if((flags & Task.NOTIFY_MODE_FIVE) > 0)
            mode.setSelection(1);
        else
            mode.setSelection(0);
    }

    public int getValue() {
        int value = 0;
        if(during.isChecked())
            value |= Task.NOTIFY_AT_DEADLINE;
        if(after.isChecked())
            value |= Task.NOTIFY_AFTER_DEADLINE;

        value &= ~(Task.NOTIFY_MODE_FIVE | Task.NOTIFY_MODE_NONSTOP);
        if(mode.getSelectedItemPosition() == 2)
            value |= Task.NOTIFY_MODE_NONSTOP;
        else if(mode.getSelectedItemPosition() == 1)
            value |= Task.NOTIFY_MODE_FIVE;

        return value;
    }

    @Override
    protected void afterInflate() {
        during = (CheckBox) getView().findViewById(R.id.reminder_due);
        after = (CheckBox) getView().findViewById(R.id.reminder_overdue);
        modeDisplay = (TextView) getView().findViewById(R.id.reminder_alarm_display);
        mode = (Spinner) getView().findViewById(R.id.reminder_alarm);
        View modeContainer = getView().findViewById(R.id.reminder_alarm_container);
        modeContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mode.performClick();
            }
        });

        randomControlSet = new RandomReminderControlSet(activity, getView(), -1);
        alarmControl = new AlarmControlSet(activity, R.layout.control_set_alarms);
        alarmControl.readFromTask(model);

        remindersBody = (LinearLayout) getView().findViewById(R.id.reminders_body);
        remindersBody.addView(alarmControl.getView());
        while (extraViews.size() > 0) {
            addViewToBody(extraViews.remove(0));
        }

        String[] list = new String[] {
                activity.getString(R.string.TEA_reminder_mode_once),
                activity.getString(R.string.TEA_reminder_mode_five),
                activity.getString(R.string.TEA_reminder_mode_nonstop),
        };
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                modeDisplay.setText(adapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mode.setAdapter(adapter);
            }
        });
    }

    @Override
    protected void readFromTaskOnInitialize() {
        setValue(model.getValue(Task.REMINDER_FLAGS));
        // Calls to get view will force other control sets to load
        randomControlSet.readFromTask(model);
        randomControlSet.readFromTaskOnInitialize();
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        task.setValue(Task.REMINDER_FLAGS, getValue());

        randomControlSet.writeToModel(task);
        alarmControl.writeToModel(task);
        return null;
    }

    @Override
    protected void refreshDisplayView() {
        int reminderCount = 0;
        StringBuilder reminderString = new StringBuilder();

        // Has random reminder?
        if ((randomControlSet != null && randomControlSet.hasRandomReminder()) || (randomControlSet == null && model.getValue(Task.REMINDER_PERIOD) > 0)) {
            reminderString.append(activity.getString(R.string.TEA_reminder_randomly_short));
            reminderCount++;
        }

        int value;
        if (initialized)
            value = getValue();
        else
            value = model.getValue(Task.REMINDER_FLAGS);

        boolean appendedWhen = false;
        if ((value & Task.NOTIFY_AT_DEADLINE) > 0) {
            if (reminderCount > 0)
                reminderString.append(" & "); //$NON-NLS-1$

            reminderString.append(activity.getString(R.string.TEA_reminder_when)).append(" "); //$NON-NLS-1$
            reminderString.append(activity.getString(R.string.TEA_reminder_due_short));
            reminderCount++;
            appendedWhen = true;
        }

        if ((value & Task.NOTIFY_AFTER_DEADLINE) > 0 && reminderCount < 2) {
            if (reminderCount > 0)
                reminderString.append(" & "); //$NON-NLS-1$

            if (!appendedWhen)
                reminderString.append(activity.getString(R.string.TEA_reminder_when)).append(" "); //$NON-NLS-1$
            reminderString.append(activity.getString(R.string.TEA_reminder_overdue_short));
            reminderCount++;
        }

        if (reminderCount > 0) {
            String toDisplay;
            if (reminderCount == 1) {
                toDisplay = activity.getString(R.string.TEA_reminder_display_one, reminderString.toString());
            } else {
                toDisplay = activity.getString(R.string.TEA_reminder_display_multiple, reminderString.toString());
            }

            label.setText(toDisplay);
            label.setTextColor(themeColor);
            image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_reminder, R.drawable.tea_icn_reminder_lightblue));
        } else {
            label.setText(R.string.TEA_reminders_group_label);
            label.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_reminder_gray);
        }
    }
}
