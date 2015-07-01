/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.NNumberPickerDialog.OnNNumberPickedListener;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

public class TimeDurationControlSet implements OnNNumberPickedListener, View.OnClickListener {

    private final Activity activity;
    private final TextView timeButton;
    private int timeDuration;
    private int[] initialValues = null;
    private NNumberPickerDialog dialog = null;
    private Task model;
    private final IntegerProperty property;
    private ActivityPreferences activityPreferences;

    public TimeDurationControlSet(Activity activity, View view, IntegerProperty property,
            int timeButtonId, ActivityPreferences activityPreferences) {
        this.activity = activity;
        this.property = property;
        this.activityPreferences = activityPreferences;

        timeButton = (TextView)view.findViewById(timeButtonId);
        ((View) timeButton.getParent()).setOnClickListener(this);
    }

    public void setModel(Task model) {
        this.model = model;
    }

    public int getTimeDurationInSeconds() {
        return timeDuration;
    }

    public void setTimeDuration(Integer timeDurationInSeconds) {
        if(timeDurationInSeconds == null) {
            timeDurationInSeconds = 0;
        }

        timeDuration = timeDurationInSeconds;

        Resources r = activity.getResources();
        if(timeDurationInSeconds == 0) {
            timeButton.setText(r.getString(R.string.WID_dateButtonUnset));
            return;
        }

        String prefix = "";
        timeButton.setText(prefix + DateUtils.formatElapsedTime(timeDuration));
        int hours = timeDuration / 3600;
        int minutes = timeDuration / 60 - 60 * hours;
        initialValues = new int[] { hours, minutes };

        if (model != null) {
            model.setValue(property, timeDuration);
        }
    }

    /** Called when NumberPicker activity is completed */
    @Override
    public void onNumbersPicked(int[] values) {
        setTimeDuration(values[0] * 3600 + values[1] * 60);
    }

    /** Called when time button is clicked */
    @Override
    public void onClick(View v) {
        if(dialog == null) {
            dialog = new NNumberPickerDialog(activity, activityPreferences.getDialogTheme(), this,
                    activity.getResources().getString(R.string.DLG_hour_minutes),
                    new int[] {0, 0}, new int[] {1, 5}, new int[] {0, 0},
                    new int[] {999, 59}, new String[] {":", null});
            final NumberPicker hourPicker = dialog.getPicker(0);
            final NumberPicker minutePicker = dialog.getPicker(1);
            minutePicker.setFormatter(new NumberPicker.Formatter() {
                @Override
                public String toString(int value) {
                    return String.format("%02d", value);
                }
            });
            minutePicker.setOnChangeListener(new NumberPicker.OnChangedListener() {
                @Override
                public int onChanged(int newVal) {
                    if(newVal < 0) {
                        if(hourPicker.getCurrent() == 0) {
                            return 0;
                        }
                        hourPicker.setCurrent(hourPicker.getCurrent() - 1);
                        return 60 + newVal;
                    } else if(newVal > 59) {
                        hourPicker.setCurrent(hourPicker.getCurrent() + 1);
                        return newVal % 60;
                    }
                    return newVal;
                }
            });
        }

        if(initialValues != null) {
            dialog.setInitialValues(initialValues);
        }

        dialog.show();
    }


}
