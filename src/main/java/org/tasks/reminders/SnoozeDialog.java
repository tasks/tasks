package org.tasks.reminders;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.ArrayAdapter;

import com.todoroo.astrid.reminders.SnoozeCallback;

import org.joda.time.DateTime;
import org.tasks.R;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.DateUtilities.getTimeString;

public class SnoozeDialog extends InjectingDialogFragment {

    public static final int REQUEST_DATE_TIME = 10101;

    @Inject Preferences preferences;
    @Inject @ForApplication Context context;

    private DateTime now = new DateTime();
    private SnoozeCallback snoozeCallback;
    private DialogInterface.OnCancelListener onCancelListener;
    private List<Long> snoozeTimes = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private DateTime getDateTimeShortcut(int resId, long def) {
        return now.withMillisOfDay(preferences.getInt(getString(resId), (int) def));
    }

    private void add(int resId, DateTime dateTime) {
        adapter.add(getString(resId, getTimeString(context, dateTime.toDate())));
        snoozeTimes.add(dateTime.getMillis());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        DateTime morning = getDateTimeShortcut(R.string.p_date_shortcut_morning, TimeUnit.HOURS.toMillis(9));
        DateTime afternoon = getDateTimeShortcut(R.string.p_date_shortcut_afternoon, TimeUnit.HOURS.toMillis(13));
        DateTime evening = getDateTimeShortcut(R.string.p_date_shortcut_evening, TimeUnit.HOURS.toMillis(17));
        DateTime night = getDateTimeShortcut(R.string.p_date_shortcut_night, TimeUnit.HOURS.toMillis(20));
        DateTime tomorrowMorning = morning.plusDays(1);
        DateTime tomorrowAfternoon = afternoon.plusDays(1);

        adapter.add(getString(R.string.date_shortcut_hour));
        snoozeTimes.add(0L);
        
        DateTime hourCutoff = new DateTime().plusMinutes(75);

        if (morning.isAfter(hourCutoff)) {
            add(R.string.date_shortcut_morning, morning);
            add(R.string.date_shortcut_afternoon, afternoon);
        } else if (afternoon.isAfter(hourCutoff)) {
            add(R.string.date_shortcut_afternoon, afternoon);
            add(R.string.date_shortcut_evening, evening);
        } else if (evening.isAfter(hourCutoff)) {
            add(R.string.date_shortcut_evening, evening);
            add(R.string.date_shortcut_night, night);
        } else if (night.isAfter(hourCutoff)) {
            add(R.string.date_shortcut_night, night);
            add(R.string.date_shortcut_tomorrow_morning, tomorrowMorning);
        } else {
            add(R.string.date_shortcut_tomorrow_morning, tomorrowMorning);
            add(R.string.date_shortcut_tomorrow_afternoon, tomorrowAfternoon);
        }
        adapter.add(getString(R.string.pick_a_date_and_time));

        return new AlertDialog.Builder(getActivity(), R.style.Tasks_Dialog)
                .setTitle(R.string.rmd_NoA_snooze)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                snoozeCallback.snoozeForTime(new DateTime().plusHours(1).getMillis());
                                break;
                            case 1:
                            case 2:
                                snoozeCallback.snoozeForTime(snoozeTimes.get(which));
                                break;
                            case 3:
                                dialog.dismiss();
                                getActivity().startActivityForResult(new Intent(context, DateAndTimePickerActivity.class) {{
                                    putExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, new DateTime().plusMinutes(30).getMillis());
                                }}, REQUEST_DATE_TIME);
                                break;
                        }
                    }
                })
                .show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (onCancelListener != null) {
            onCancelListener.onCancel(dialog);
        }
    }

    public void setSnoozeCallback(SnoozeCallback snoozeCallback) {
        this.snoozeCallback = snoozeCallback;
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
    }
}