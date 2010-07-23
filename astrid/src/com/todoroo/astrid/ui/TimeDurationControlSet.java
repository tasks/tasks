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
import android.view.View;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.ui.NNumberPickerDialog.OnNNumberPickedListener;

@SuppressWarnings("nls")
public class TimeDurationControlSet implements OnNNumberPickedListener,
        View.OnClickListener {

    @Autowired
    DateUtilities dateUtilities;

    public enum TimeDurationType {
        DAYS_HOURS,
        HOURS_MINUTES;
    }

    private final Activity activity;
    private final Button timeButton;
    private final NNumberPickerDialog dialog;
    private final int prefixResource;
    private final TimeDurationType type;
    private int timeDuration;

    public TimeDurationControlSet(Activity activity, int timeButtonId,
            int prefixResource, int titleResource, TimeDurationType type) {
        Resources r = activity.getResources();
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        this.prefixResource = prefixResource;
        this.type = type;

        timeButton = (Button)activity.findViewById(timeButtonId);
        timeButton.setOnClickListener(this);

        switch(type) {
        case DAYS_HOURS:
            dialog = new NNumberPickerDialog(activity, this,
                    activity.getResources().getString(titleResource),
                    new int[] {0, 0}, new int[] {1, 1}, new int[] {0, 0},
                    new int[] {31, 23}, new String[] {
                        r.getString(R.string.daysVertical),
                        r.getString(R.string.hoursVertical)
                    });
            break;
        case HOURS_MINUTES:
        default:
            dialog = new NNumberPickerDialog(activity, this,
                    activity.getResources().getString(titleResource),
                    new int[] {0, 0}, new int[] {1, 5}, new int[] {0, 0},
                    new int[] {99, 59}, new String[] {":", null});
            break;
        }
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
            timeButton.setText(r.getString(R.string.blank_button_title));
            return;
        }

        String prefix = "";
        if(prefixResource != 0)
            prefix = r.getString(prefixResource);
        timeButton.setText(prefix + " " + dateUtilities.getDurationString(
                timeDurationInSeconds * 1000L, 2));
        switch(type) {
        case DAYS_HOURS:
            int days = timeDuration / 24 / 3600;
            int hours = timeDuration / 3600 - 24 * days;
            dialog.setInitialValues(new int[] {days, hours});
            break;
        case HOURS_MINUTES:
             hours = timeDuration / 3600;
            int minutes = timeDuration/60 - 60 * hours;
            dialog.setInitialValues(new int[] {hours, minutes});
        }
    }

    /** Called when NumberPicker activity is completed */
    public void onNumbersPicked(int[] values) {
        switch(type) {
        case DAYS_HOURS:
            setTimeDuration(values[0] * 24 * 3600 + values[1] * 3600);
            break;
        case HOURS_MINUTES:
            setTimeDuration(values[0] * 3600 + values[1] * 60);
            break;
        }
    }

    /** Called when time button is clicked */
    public void onClick(View v) {
        dialog.show();
    }


}