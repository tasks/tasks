package com.todoroo.astrid.reminders;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.ui.NumberPicker;

import org.tasks.R;

public class SnoozeDialog extends FrameLayout implements DialogInterface.OnClickListener {

    LinearLayout snoozePicker;
    NumberPicker snoozeValue;
    Spinner snoozeUnits;
    SnoozeCallback snoozeCallback;

    public SnoozeDialog(Activity activity, SnoozeCallback callback) {
        super(activity);
        this.snoozeCallback = callback;

        LayoutInflater mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.snooze_dialog, this, true);

        snoozePicker = (LinearLayout) findViewById(R.id.snoozePicker);
        snoozeValue = (NumberPicker) findViewById(R.id.numberPicker);
        snoozeUnits = (Spinner) findViewById(R.id.numberUnits);

        snoozeValue.setIncrementBy(1);
        snoozeValue.setRange(1, 99);
        snoozeUnits.setSelection(RepeatControlSet.INTERVAL_HOURS);
        snoozeUnits.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AndroidUtilities.hideSoftInputForViews(getContext(), snoozePicker);
                return false;
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        long time = DateUtilities.now();
        int value = snoozeValue.getCurrent();
        switch(snoozeUnits.getSelectedItemPosition()) {
            case RepeatControlSet.INTERVAL_DAYS:
                time += value * DateUtilities.ONE_DAY;
                break;
            case RepeatControlSet.INTERVAL_HOURS:
                time += value * DateUtilities.ONE_HOUR;
                break;
            case RepeatControlSet.INTERVAL_MINUTES:
                time += value * DateUtilities.ONE_MINUTE;
                break;
            case RepeatControlSet.INTERVAL_WEEKS:
                time += value * 7 * DateUtilities.ONE_DAY;
                break;
            case RepeatControlSet.INTERVAL_MONTHS:
                time = DateUtilities.addCalendarMonthsToUnixtime(time, 1);
                break;
            case RepeatControlSet.INTERVAL_YEARS:
                time = DateUtilities.addCalendarMonthsToUnixtime(time, 12);
                break;
        }

        snoozeCallback.snoozeForTime(time);
    }
}