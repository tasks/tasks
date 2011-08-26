package com.todoroo.astrid.ui;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;

/**
 * Control set dealing with reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderControlSet implements TaskEditControlSet {
    private final CheckBox during, after;
    private final Spinner mode;

    public ReminderControlSet(Activity activity, int duringId, int afterId, int modeId) {
        during = (CheckBox)activity.findViewById(duringId);
        after = (CheckBox)activity.findViewById(afterId);
        mode = (Spinner)activity.findViewById(modeId);

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

    public void setValue(int flags) {
        during.setChecked((flags & Task.NOTIFY_AT_DEADLINE) > 0);
        after.setChecked((flags &
                Task.NOTIFY_AFTER_DEADLINE) > 0);

        int rmd_mode_default = Preferences.getIntegerFromString(R.string.p_default_reminders_mode_key, 0);

        if((flags & Task.NOTIFY_MODE_NONSTOP) > 0)
            mode.setSelection(2);
        else if((flags & Task.NOTIFY_MODE_FIVE) > 0)
            mode.setSelection(1);
        else if((rmd_mode_default & Task.NOTIFY_MODE_NONSTOP) > 0)
            mode.setSelection(2);
        else if((rmd_mode_default & Task.NOTIFY_MODE_FIVE) > 0)
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
    public void readFromTask(Task task) {
        setValue(task.getValue(Task.REMINDER_FLAGS));
    }

    @Override
    public String writeToModel(Task task) {
        task.setValue(Task.REMINDER_FLAGS, getValue());
        // clear snooze if task is being edited
        task.setValue(Task.REMINDER_SNOOZE, 0L);
        return null;
    }
}