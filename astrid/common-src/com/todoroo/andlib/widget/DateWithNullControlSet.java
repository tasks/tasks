/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.widget;

import java.util.Date;

import android.app.Activity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/** Date Control Set with an "enabled" checkbox" to toggle date / null */
public class DateWithNullControlSet extends DateControlSet {

    private CheckBox activatedCheckBox;

    public DateWithNullControlSet(Activity activity, int checkBoxId, int dateButtonId, int timeButtonId) {
        super(activity);
        activatedCheckBox = (CheckBox)activity.findViewById(checkBoxId);
        dateButton = (Button)activity.findViewById(dateButtonId);
        timeButton = (Button)activity.findViewById(timeButtonId);

        activatedCheckBox.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                dateButton.setEnabled(isChecked);
                timeButton.setEnabled(isChecked);
            }
        });
        dateButton.setOnClickListener(this);
        timeButton.setOnClickListener(this);
    }

    @Override
    public Date getDate() {
        if(!activatedCheckBox.isChecked())
            return null;
        return super.getDate();
    }

    /** Initialize the components for the given date field */
    @Override
    public void setDate(Date newDate) {
        activatedCheckBox.setChecked(newDate != null);
        dateButton.setEnabled(newDate != null);
        timeButton.setEnabled(newDate != null);

        super.setDate(newDate);
    }
}
