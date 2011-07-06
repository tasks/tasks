package com.todoroo.astrid.ui;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;

/**
 * Control set dealing with random reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RandomReminderControlSet implements TaskEditControlSet {
    /** default interval for spinner if date is unselected */
    private final long DEFAULT_INTERVAL = DateUtilities.ONE_WEEK * 2;

    private final CheckBox settingCheckbox;
    private final Spinner periodSpinner;

    private boolean periodSpinnerInitialized = false;
    private final int[] hours;

    public RandomReminderControlSet(Activity activity, int settingCheckboxId, int periodButtonId) {
        settingCheckbox = (CheckBox)activity.findViewById(settingCheckboxId);
        periodSpinner = (Spinner)activity.findViewById(periodButtonId);
        periodSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                if(periodSpinnerInitialized)
                    settingCheckbox.setChecked(true);
                periodSpinnerInitialized = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // ignore
            }
        });

        // create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity, android.R.layout.simple_spinner_item,
                activity.getResources().getStringArray(R.array.TEA_reminder_random));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);

        // create hour array
        String[] hourStrings = activity.getResources().getStringArray(R.array.TEA_reminder_random_hours);
        hours = new int[hourStrings.length];
        for(int i = 0; i < hours.length; i++)
            hours[i] = Integer.parseInt(hourStrings[i]);
    }

    @Override
    public void readFromTask(Task task) {
        long time = task.getValue(Task.REMINDER_PERIOD);

        boolean enabled = time > 0;
        if(time <= 0) {
            time = DEFAULT_INTERVAL;
        }

        int i;
        for(i = 0; i < hours.length - 1; i++)
            if(hours[i] * DateUtilities.ONE_HOUR >= time)
                break;
        periodSpinner.setSelection(i);
        settingCheckbox.setChecked(enabled);
    }

    @Override
    public String writeToModel(Task task) {
        if(settingCheckbox.isChecked()) {
            int hourValue = hours[periodSpinner.getSelectedItemPosition()];
            task.setValue(Task.REMINDER_PERIOD, hourValue * DateUtilities.ONE_HOUR);
        } else
            task.setValue(Task.REMINDER_PERIOD, 0L);
        return null;
    }
}