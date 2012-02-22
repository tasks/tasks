/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
