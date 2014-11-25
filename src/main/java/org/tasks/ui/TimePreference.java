package org.tasks.ui;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.joda.time.DateTime;

public class TimePreference extends DialogPreference {

    private int millisOfDay;
    private TimePicker picker = null;
    private int defaultFocusability;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    public View onCreateDialogView() {
        picker = new TimePicker(getContext());
        defaultFocusability = picker.getDescendantFocusability();
        refreshPicker();
        return picker;
    }

    @Override
    public void onBindDialogView(View v) {
        super.onBindDialogView(v);

        refreshPicker();
    }

    private void refreshPicker() {
        DateTime dateTime = DateTime.now().withMillisOfDay(millisOfDay);
        picker.setCurrentHour(dateTime.getHourOfDay());
        picker.setCurrentMinute(dateTime.getMinuteOfHour());
        picker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        if(picker.is24HourView()) {
            // Disable keyboard input on time picker to avoid this:
            // https://code.google.com/p/android/issues/detail?id=24387
            picker.setDescendantFocusability(TimePicker.FOCUS_BLOCK_DESCENDANTS);
        } else {
            picker.setDescendantFocusability(defaultFocusability);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            picker.clearFocus();
            millisOfDay = new DateTime()
                    .withMillisOfDay(0)
                    .withHourOfDay(picker.getCurrentHour())
                    .withMinuteOfHour(picker.getCurrentMinute())
                    .getMillisOfDay();

            if (callChangeListener(millisOfDay)) {
                persistInt(millisOfDay);
            }
        }
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int def = (defaultValue == null) ? 0 : (int) defaultValue;

        if (restoreValue) {
            millisOfDay = getPersistedInt(def);
        } else {
            millisOfDay = def;
        }
    }

    public int getMillisOfDay() {
        return millisOfDay;
    }
}
