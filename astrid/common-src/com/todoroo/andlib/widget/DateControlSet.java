/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.widget;

import java.text.Format;
import java.util.Date;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.todoroo.astrid.ui.CalendarDialog;

public class DateControlSet implements OnTimeSetListener,
        OnDateSetListener, View.OnClickListener {

    private final Format dateFormatter;
    private final Format timeFormatter;

    protected final Context context;
    protected Button dateButton;
    protected Button timeButton;
    protected Date date;

    protected DateControlSet(Context context) {
        this.context = context;

        dateFormatter = DateFormat.getDateFormat(context);
        timeFormatter = DateFormat.getTimeFormat(context);
    }

    public DateControlSet(Context context, Button dateButton, Button timeButton) {
        this(context);

        this.dateButton = dateButton;
        this.timeButton = timeButton;

        if(dateButton != null)
            dateButton.setOnClickListener(this);

        if(timeButton != null)
            timeButton.setOnClickListener(this);

        setDate(null);
    }

    public Date getDate() {
        return date;
    }

    /** Initialize the components for the given date field */
    public void setDate(Date newDate) {
        if(newDate == null) {
            date = new Date();
        } else {
            this.date = new Date(newDate.getTime());
        }

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
        if(dateButton != null)
            dateButton.setText(dateFormatter.format(date));

    }

    public void updateTime() {
        if(timeButton != null)
            timeButton.setText(timeFormatter.format(date));
    }

    private boolean dialogCancelled = false;

    public void onClick(View v) {
        if(v == timeButton)
            new TimePickerDialog(context, this, date.getHours(),
                date.getMinutes(), false).show();
        else if(v == dateButton){
            final CalendarDialog calendarDialog = new CalendarDialog(context, date);
            calendarDialog.show();
            calendarDialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface arg0) {
                    if (!dialogCancelled) {
                        date = calendarDialog.getCalendarDate();
                        updateDate();
                    }
                    dialogCancelled = false;
                }
            });

            calendarDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    dialogCancelled = true;
                }
            });
        }
    }
}
