package com.todoroo.astrid.ui;

import java.util.Calendar;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;

public class AstridTimePicker extends LinearLayout {

    private final ToggleButton noTimeCheck;
    private final ToggleButton amButton;
    private final ToggleButton pmButton;
    private final NumberPicker hours;
    private final NumberPicker minutes;
    private TimePickerEnabledChangedListener listener;
    private boolean is24Hour;

    private boolean lastSelectionWasPm; // false for AM, true for PM

    public interface TimePickerEnabledChangedListener {
        public void timePickerEnabledChanged(boolean hasTime);
    }



    public AstridTimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.astrid_time_picker, this, true);

        noTimeCheck = (ToggleButton) findViewById(R.id.hasTime);
        amButton= (ToggleButton) findViewById(R.id.am_button);
        pmButton= (ToggleButton) findViewById(R.id.pm_button);
        hours = (NumberPicker) findViewById(R.id.hours);
        minutes = (NumberPicker) findViewById(R.id.minutes);

        setupButtonBackgrounds(context);

        initialize(context);
    }

    private void setupButtonBackgrounds(Context context) {
        Resources r = context.getResources();
        TypedValue onColor = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.asThemeTextColor, onColor, false);

        int onColorValue = r.getColor(onColor.data);
        int offColorValue = r.getColor(android.R.color.transparent);
        int borderColorValue = r.getColor(R.color.task_edit_deadline_gray);
        int cornerRadius = (int) (5 * r.getDisplayMetrics().density);
        int strokeWidth = (int) (2 * r.getDisplayMetrics().density);

        amButton.setBackgroundDrawable(CustomBorderDrawable.customButton(cornerRadius, 0, 0, cornerRadius,
                onColorValue, offColorValue, borderColorValue, strokeWidth));
        pmButton.setBackgroundDrawable(CustomBorderDrawable.customButton(0, cornerRadius, cornerRadius, 0,
                onColorValue, offColorValue, borderColorValue, strokeWidth));
        noTimeCheck.setBackgroundDrawable(CustomBorderDrawable.customButton(cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                onColorValue, offColorValue, borderColorValue, strokeWidth));

        hours.findViewById(R.id.increment).setBackgroundDrawable(
                CustomBorderDrawable.customButton(cornerRadius, 0, 0, 0, onColorValue, offColorValue, borderColorValue, strokeWidth));
        hours.findViewById(R.id.decrement).setBackgroundDrawable(
                CustomBorderDrawable.customButton(0, 0, 0, cornerRadius, onColorValue, offColorValue, borderColorValue, strokeWidth));

        minutes.findViewById(R.id.increment).setBackgroundDrawable(
                CustomBorderDrawable.customButton(0, cornerRadius, 0, 0, onColorValue, offColorValue, borderColorValue, strokeWidth));
        minutes.findViewById(R.id.decrement).setBackgroundDrawable(
                CustomBorderDrawable.customButton(0, 0, cornerRadius, 0, onColorValue, offColorValue, borderColorValue, strokeWidth));
    }

    private void initialize(Context context) {
        if (DateUtilities.is24HourFormat(context)) {
            hours.setRange(0, 23);
            is24Hour = true;
            findViewById(R.id.am_pm_container).setVisibility(View.GONE);
        } else {
            hours.setRange(1, 12);
            is24Hour = false;
        }
        minutes.setRange(0, 59);

        NumberPicker.OnChangedListener autoEnableTimeCheck = new NumberPicker.OnChangedListener() {

            @Override
            public int onChanged(NumberPicker picker, int oldVal, int newVal) {
                setHasTime(true);
                return newVal;
            }
        };

        hours.findViewById(R.id.timepicker_left_border).setVisibility(View.VISIBLE);
        minutes.findViewById(R.id.timepicker_right_border).setVisibility(View.VISIBLE);

        String amString = DateUtils.getAMPMString(Calendar.AM).toLowerCase();
        amButton.setTextOff(amString);
        amButton.setTextOn(amString);
        amButton.setChecked(false);

        String pmString = DateUtils.getAMPMString(Calendar.PM).toLowerCase();
        pmButton.setTextOff(pmString);
        pmButton.setTextOn(pmString);
        pmButton.setChecked(false);

        amButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                noTimeCheck.setChecked(false);
                amButton.setChecked(true);
                pmButton.setChecked(false);
                lastSelectionWasPm = false;
            }
        });

        pmButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                noTimeCheck.setChecked(false);
                amButton.setChecked(false);
                pmButton.setChecked(true);
                lastSelectionWasPm = true;
            }
        });

        String noTime = context.getString(R.string.TEA_no_time).toLowerCase();
        noTimeCheck.setTextOff(noTime);
        noTimeCheck.setTextOn(noTime);

        hours.setOnChangeListener(autoEnableTimeCheck);
        minutes.setOnChangeListener(autoEnableTimeCheck);

        noTimeCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setHasTime(!isChecked, false);
            }
        });

        minutes.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
    }

    public void setHasTime(boolean hasTime) {
        setHasTime(hasTime, true);
    }

    public void setHasTime(boolean hasTime, boolean setChecked) {
        if (setChecked)
            noTimeCheck.setChecked(!hasTime);


        if (noTimeCheck.isChecked()) {
            hours.setText(""); //$NON-NLS-1$
            minutes.setText(""); //$NON-NLS-1$

            lastSelectionWasPm = pmButton.isChecked();
            amButton.setChecked(false);
            pmButton.setChecked(false);
        } else {
            hours.validateAndUpdate();
            minutes.validateAndUpdate();

            amButton.setChecked(!lastSelectionWasPm);
            pmButton.setChecked(lastSelectionWasPm);
        }

        if (listener != null)
            listener.timePickerEnabledChanged(hasTime);
    }

    public boolean hasTime() {
        return !noTimeCheck.isChecked();
    }

    public void forceNoTime() {
        if (!noTimeCheck.isChecked())
            noTimeCheck.performClick();
    }

    public void setHours(int hour) {
        boolean pm = false;
        if (!is24Hour) {
            if (hour == 0) {
                hour = 12;
            } else if (hour == 12) {
                pm = true;
            } else if (hour > 12) {
                hour -= 12;
                pm = true;
            }
        }
        amButton.setChecked(!pm);
        pmButton.setChecked(pm);
        lastSelectionWasPm = pm;
        hours.setCurrent(hour);
    }

    public int getHours() {
        int toReturn = hours.getCurrent();
        if (!is24Hour) {
            if (toReturn == 12) {
                if (amButton.isChecked())
                    toReturn = 0;
            } else if (pmButton.isChecked()) {
                toReturn += 12;
            }
        }
        return toReturn;
    }

    public void setMinutes(int minute) {
        minutes.setCurrent(minute);
    }

    public int getMinutes() {
        return minutes.getCurrent();
    }

    public void setTimePickerEnabledChangedListener(TimePickerEnabledChangedListener listener) {
        this.listener = listener;
    }

}
