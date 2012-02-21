package com.todoroo.astrid.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.timsu.astrid.R;
import com.todoroo.astrid.alarms.AlarmControlSet;
import com.todoroo.astrid.data.Task;

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderControlSet extends PopupControlSet {
    private CheckBox during, after;
    private Spinner mode;
    private LinearLayout remindersBody;
    private final List<View> extraViews;

    private RandomReminderControlSet randomControlSet;
    private AlarmControlSet alarmControl;

    public ReminderControlSet(Activity activity, int viewLayout, int displayViewLayout) {
        super(activity, viewLayout, displayViewLayout, R.string.TEA_reminders_group_label);
        extraViews = new ArrayList<View>();
        displayText.setText(activity.getString(R.string.TEA_reminders_group_label));
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
        mode = (Spinner) getView().findViewById(R.id.reminder_alarm);

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
        // clear snooze if task is being edited
        task.setValue(Task.REMINDER_SNOOZE, 0L);

        randomControlSet.writeToModel(task);
        alarmControl.writeToModel(task);
        return null;
    }

    @Override
    protected void refreshDisplayView() {
        // Nothing to do here
    }
}
