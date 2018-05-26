/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.todoroo.andlib.utility.DateUtilities;
import org.tasks.R;

/**
 * Control set dealing with random reminder settings
 *
 * @author Tim Su <tim@todoroo.com>
 */
class RandomReminderControlSet {

  private final int[] hours;
  private int selectedIndex;

  public RandomReminderControlSet(Context context, View parentView, long reminderPeriod) {
    Spinner periodSpinner = parentView.findViewById(R.id.reminder_random_interval);
    periodSpinner.setVisibility(View.VISIBLE);
    // create adapter
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            context.getResources().getStringArray(R.array.TEA_reminder_random));
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    periodSpinner.setAdapter(adapter);

    periodSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            selectedIndex = position;
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    // create hour array
    String[] hourStrings = context.getResources().getStringArray(R.array.TEA_reminder_random_hours);
    hours = new int[hourStrings.length];
    for (int i = 0; i < hours.length; i++) {
      hours[i] = Integer.parseInt(hourStrings[i]);
    }

    int i;
    for (i = 0; i < hours.length - 1; i++) {
      if (hours[i] * DateUtilities.ONE_HOUR >= reminderPeriod) {
        break;
      }
    }
    periodSpinner.setSelection(i);
  }

  public long getReminderPeriod() {
    int hourValue = hours[selectedIndex];
    return hourValue * DateUtilities.ONE_HOUR;
  }
}
