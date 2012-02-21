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
package com.todoroo.astrid.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.ui.NNumberPickerDialog.OnNNumberPickedListener;

@SuppressWarnings("nls")
public class TimeDurationControlSet implements OnNNumberPickedListener,
        View.OnClickListener {

    private final Activity activity;
    private final Button timeButton;
    private final int prefixResource;
    private int timeDuration;
    private int[] initialValues = null;
    private final int titleResource;
    private NNumberPickerDialog dialog = null;

    public TimeDurationControlSet(Activity activity, View view, int timeButtonId,
            int prefixResource, int titleResource) {
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        this.prefixResource = prefixResource;
        this.titleResource = titleResource;

        timeButton = (Button)view.findViewById(timeButtonId);
        timeButton.setOnClickListener(this);
    }

    public int getTimeDurationInSeconds() {
        return timeDuration;
    }

    public void setTimeDuration(Integer timeDurationInSeconds) {
        if(timeDurationInSeconds == null)
            timeDurationInSeconds = 0;

        timeDuration = timeDurationInSeconds;

        Resources r = activity.getResources();
        if(timeDurationInSeconds == 0) {
            timeButton.setText(r.getString(R.string.WID_dateButtonUnset));
            return;
        }

        String prefix = "";
        if (prefixResource != 0)
            prefix = r.getString(prefixResource) + " ";
        timeButton.setText(prefix + DateUtils.formatElapsedTime(timeDuration));
        int hours = timeDuration / 3600;
        int minutes = timeDuration / 60 - 60 * hours;
        initialValues = new int[] { hours, minutes };
    }

    /** Called when NumberPicker activity is completed */
    public void onNumbersPicked(int[] values) {
        setTimeDuration(values[0] * 3600 + values[1] * 60);
    }

    /** Called when time button is clicked */
    public void onClick(View v) {
        if(dialog == null) {
            dialog = new NNumberPickerDialog(activity, this,
                    activity.getResources().getString(titleResource),
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
                public int onChanged(NumberPicker picker, int oldVal, int newVal) {
                    if(newVal < 0) {
                        if(hourPicker.getCurrent() == 0)
                            return 0;
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

        if(initialValues != null)
            dialog.setInitialValues(initialValues);

        dialog.show();
    }


}
