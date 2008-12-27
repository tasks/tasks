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
package com.timsu.astrid.widget;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class DateControlSet implements OnTimeSetListener,
        OnDateSetListener, View.OnClickListener {

    private static final Format dateFormatter = new SimpleDateFormat("EEE, MMM d, yyyy");
    private static final Format timeFormatter = new SimpleDateFormat("h:mm a");

    private final Activity activity;
    private CheckBox activatedCheckBox;
    private Button dateButton;
    private Button timeButton;
    private Date date;

    public DateControlSet(Activity activity, int checkBoxId, int dateButtonId, int timeButtonId) {
        this.activity = activity;
        activatedCheckBox = (CheckBox)activity.findViewById(checkBoxId);
        dateButton = (Button)activity.findViewById(dateButtonId);
        timeButton = (Button)activity.findViewById(timeButtonId);

        activatedCheckBox.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                dateButton.setEnabled(isChecked);
                timeButton.setEnabled(isChecked);
            }
        });
        dateButton.setOnClickListener(this);
        timeButton.setOnClickListener(this);
    }

    public Date getDate() {
        if(!activatedCheckBox.isChecked())
            return null;
        return date;
    }

    /** Initialize the components for the given date field */
    public void setDate(Date newDate) {
        this.date = newDate;
        if(newDate == null) {
            date = new Date();
            date.setTime(date.getTime() + 24*3600*1000);
            date.setMinutes(0);
        }

        activatedCheckBox.setChecked(newDate != null);
        dateButton.setEnabled(newDate != null);
        timeButton.setEnabled(newDate != null);

        updateDate();
        updateTime();
    }

    public void onDateSet(DatePicker view, int year, int month, int monthDay) {
        date.setYear(year - 1900);
        date.setMonth(month);
        date.setDate(monthDay);
        updateDate();
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        date.setHours(hourOfDay);
        date.setMinutes(minute);
        updateTime();
    }

    public void updateDate() {
        dateButton.setText(dateFormatter.format(date));

    }

    public void updateTime() {
        timeButton.setText(timeFormatter.format(date));
    }

    public void onClick(View v) {
        if(v == timeButton)
            new TimePickerDialog(activity, this, date.getHours(),
                date.getMinutes(), false).show();
        else
            new DatePickerDialog(activity, this, 1900 +
                    date.getYear(), date.getMonth(), date.getDate()).show();
    }
}