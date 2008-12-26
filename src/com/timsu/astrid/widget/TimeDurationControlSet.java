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

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;

import com.timsu.astrid.R;
import com.timsu.astrid.utilities.DateUtilities;
import com.timsu.astrid.widget.NumberPickerDialog.OnNumberPickedListener;

public class TimeDurationControlSet implements OnNumberPickedListener,
        View.OnClickListener {

    private final Activity activity;
    private Button timeButton;
    private int timeDuration;
    private final NumberPickerDialog dialog;

    public TimeDurationControlSet(Activity activity, int timeButtonId) {
        this.activity = activity;
        timeButton = (Button)activity.findViewById(timeButtonId);
        timeButton.setOnClickListener(this);
        dialog = new NumberPickerDialog(activity, this,
                activity.getResources().getString(R.string.minutes_dialog),
                0, 5, 0, 999);
    }

    public int getTimeDurationInSeconds() {
        return timeDuration;
    }

    public void setTimeElapsed(Integer timeDurationInSeconds) {
        if(timeDurationInSeconds == null)
            timeDurationInSeconds = 0;

        timeDuration = timeDurationInSeconds;

        Resources r = activity.getResources();
        if(timeDurationInSeconds == 0) {
            timeButton.setText(r.getString(R.string.blank_button_title));
            return;
        }

        timeButton.setText(DateUtilities.getDurationString(r,
                timeDurationInSeconds, 2));
        dialog.setInitialValue(timeDuration/60);
    }

    @Override
    /** Called when NumberPicker activity is completed */
    public void onNumberPicked(NumberPicker view, int value) {
        setTimeElapsed(value * 60);
    }

    /** Called when time button is clicked */
    public void onClick(View v) {
        dialog.show();
    }


}