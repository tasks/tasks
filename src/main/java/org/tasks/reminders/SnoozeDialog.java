package org.tasks.reminders;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.todoroo.astrid.reminders.SnoozeCallback;

import org.tasks.time.DateTime;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.DateUtilities.getTimeString;

public class SnoozeDialog extends InjectingDialogFragment {

    @Inject Preferences preferences;
    @Inject @ForApplication Context context;
    @Inject DialogBuilder dialogBuilder;

    private DateTime now = new DateTime();
    private SnoozeCallback snoozeCallback;
    private DialogInterface.OnCancelListener onCancelListener;
    private List<Long> snoozeTimes = new ArrayList<>();
    private List<String> items = new ArrayList<>();

    private void add(int resId, DateTime dateTime) {
        items.add(String.format("%s (%s)", getString(resId), getTimeString(context, dateTime)));
        snoozeTimes.add(dateTime.getMillis());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DateTime morning = now.withMillisOfDay(preferences.getDateShortcutMorning());
        DateTime afternoon = now.withMillisOfDay(preferences.getDateShortcutAfternoon());
        DateTime evening = now.withMillisOfDay(preferences.getDateShortcutEvening());
        DateTime night = now.withMillisOfDay(preferences.getDateShortcutNight());
        DateTime tomorrowMorning = morning.plusDays(1);
        DateTime tomorrowAfternoon = afternoon.plusDays(1);

        items.add(getString(R.string.date_shortcut_hour));
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
        items.add(getString(R.string.pick_a_date_and_time));

        return dialogBuilder.newDialog()
                .setTitle(R.string.rmd_NoA_snooze)
                .setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
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
                                snoozeCallback.pickDateTime();
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