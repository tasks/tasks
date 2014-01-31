package com.todoroo.astrid.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.joda.time.DateTime;

/**
 * Preference dialog that displays a TimePicker and persists the selected value.
 *
 * The xml to use it is of the form:
 < com.todoroo.astrid.ui.TimePreference
     android:key="@string/my_key_value"
     android:defaultValue="-1"
     android:positiveButtonText="Save"
     android:negativeButtonText="Reset"
     android:title="@string/my_pref_title_value" />
 */
public class TimePreference extends DialogPreference {

    /** The last hour digit picked by the user in String format */
    private int millisOfDay;
    private TimePicker picker = null;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

    }

    @Override
    public View onCreateDialogView() {
        picker = new TimePicker(getContext());

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

    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        /** When the dialog is closed update the lastHour variable and store the value in preferences */
        if (positiveResult) {
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
    public Object onGetDefaultValue(TypedArray array, int index) {
        return (array.getInt(index, 0));
    }

    /** When called for the first time initialize the value of the last hour to either the saved one
     * or to the default one. If a default one is not provided use "0" */
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
