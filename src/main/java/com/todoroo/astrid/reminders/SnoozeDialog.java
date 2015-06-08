package com.todoroo.astrid.reminders;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.repeats.RepeatControlSet;
import com.todoroo.astrid.ui.NumberPicker;

import org.tasks.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SnoozeDialog extends DialogFragment {

    @InjectView(R.id.snoozePicker) LinearLayout snoozePicker;
    @InjectView(R.id.numberPicker) NumberPicker snoozeValue;
    @InjectView(R.id.numberUnits) Spinner snoozeUnits;

    private SnoozeCallback snoozeCallback;
    private DialogInterface.OnDismissListener onDismissListener;
    private String title;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = mInflater.inflate(R.layout.snooze_dialog, null);
        ButterKnife.inject(this, layout);

        snoozeValue.setIncrementBy(1);
        snoozeValue.setRange(1, 99);
        snoozeUnits.setSelection(RepeatControlSet.INTERVAL_HOURS);
        snoozeUnits.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AndroidUtilities.hideSoftInputForViews(getActivity(), snoozePicker);
                return false;
            }
        });

        return new AlertDialog.Builder(getActivity(), R.style.Tasks_Dialog)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long time = DateUtilities.now();
                        int value = snoozeValue.getCurrent();
                        switch (snoozeUnits.getSelectedItemPosition()) {
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
                })
                .setOnDismissListener(this)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (onDismissListener != null) {
            onDismissListener.onDismiss(dialog);
        }
    }

    public void setSnoozeCallback(SnoozeCallback snoozeCallback) {
        this.snoozeCallback = snoozeCallback;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}