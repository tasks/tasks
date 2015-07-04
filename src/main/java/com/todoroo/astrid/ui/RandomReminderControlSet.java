/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;

/**
 * Control set dealing with random reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RandomReminderControlSet {

    private final Spinner periodSpinner;

    private final int[] hours;

    public RandomReminderControlSet(Activity activity, View parentView) {
        periodSpinner = (Spinner) parentView.findViewById(R.id.reminder_random_interval);
        periodSpinner.setVisibility(View.VISIBLE);
        // create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item,
                activity.getResources().getStringArray(R.array.TEA_reminder_random));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);

        // create hour array
        String[] hourStrings = activity.getResources().getStringArray(R.array.TEA_reminder_random_hours);
        hours = new int[hourStrings.length];
        for(int i = 0; i < hours.length; i++) {
            hours[i] = Integer.parseInt(hourStrings[i]);
        }
    }

    public void readFromTaskOnInitialize(Task model) {
        long time = model.getReminderPeriod();

        if(time <= 0) {
            /* default interval for spinner if date is unselected */
            time = DateUtilities.ONE_WEEK * 2;
        }

        int i;
        for(i = 0; i < hours.length - 1; i++) {
            if (hours[i] * DateUtilities.ONE_HOUR >= time) {
                break;
            }
        }
        periodSpinner.setSelection(i);
    }

    public long getReminderPeriod() {
        int hourValue = hours[periodSpinner.getSelectedItemPosition()];
        return hourValue * DateUtilities.ONE_HOUR;
    }
}
