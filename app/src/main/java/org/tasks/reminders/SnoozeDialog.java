package org.tasks.reminders;

import static com.todoroo.andlib.utility.DateUtilities.getTimeString;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

public class SnoozeDialog extends InjectingDialogFragment {

  private final List<String> items = new ArrayList<>();
  @Inject Preferences preferences;
  @Inject @ForApplication Context context;
  @Inject DialogBuilder dialogBuilder;
  private SnoozeCallback snoozeCallback;
  private DialogInterface.OnCancelListener onCancelListener;

  public static List<SnoozeOption> getSnoozeOptions(Preferences preferences) {
    DateTime now = new DateTime();
    DateTime morning = now.withMillisOfDay(preferences.getDateShortcutMorning());
    DateTime afternoon = now.withMillisOfDay(preferences.getDateShortcutAfternoon());
    DateTime evening = now.withMillisOfDay(preferences.getDateShortcutEvening());
    DateTime night = now.withMillisOfDay(preferences.getDateShortcutNight());
    DateTime tomorrowMorning = morning.plusDays(1);
    DateTime tomorrowAfternoon = afternoon.plusDays(1);

    DateTime hourCutoff = new DateTime().plusMinutes(75);

    List<SnoozeOption> snoozeOptions = new ArrayList<>();

    DateTime oneHour = now.plusHours(1).withSecondOfMinute(0).withMillisOfSecond(0);
    snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_hour, oneHour));

    if (morning.isAfter(hourCutoff)) {
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_morning, morning));
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_afternoon, afternoon));
    } else if (afternoon.isAfter(hourCutoff)) {
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_afternoon, afternoon));
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_evening, evening));
    } else if (evening.isAfter(hourCutoff)) {
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_evening, evening));
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_night, night));
    } else if (night.isAfter(hourCutoff)) {
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_night, night));
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_tomorrow_morning, tomorrowMorning));
    } else {
      snoozeOptions.add(new SnoozeOption(R.string.date_shortcut_tomorrow_morning, tomorrowMorning));
      snoozeOptions.add(
          new SnoozeOption(R.string.date_shortcut_tomorrow_afternoon, tomorrowAfternoon));
    }

    return snoozeOptions;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final List<SnoozeOption> snoozeOptions = getSnoozeOptions(preferences);

    for (SnoozeOption snoozeOption : snoozeOptions) {
      items.add(
          String.format(
              "%s (%s)",
              getString(snoozeOption.getResId()),
              getTimeString(context, snoozeOption.getDateTime())));
    }

    items.add(getString(R.string.pick_a_date_and_time));

    return dialogBuilder
        .newDialog()
        .setTitle(R.string.rmd_NoA_snooze)
        .setItems(
            items,
            (dialog, which) -> {
              switch (which) {
                case 0:
                case 1:
                case 2:
                  snoozeCallback.snoozeForTime(snoozeOptions.get(which).getDateTime());
                  break;
                case 3:
                  dialog.dismiss();
                  snoozeCallback.pickDateTime();
                  break;
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

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
