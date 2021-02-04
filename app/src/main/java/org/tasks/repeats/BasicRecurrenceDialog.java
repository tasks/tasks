package org.tasks.repeats;

import static android.app.Activity.RESULT_OK;
import static com.google.common.collect.Lists.newArrayList;
import static net.fortuna.ical4j.model.Recur.Frequency.DAILY;
import static net.fortuna.ical4j.model.Recur.Frequency.HOURLY;
import static net.fortuna.ical4j.model.Recur.Frequency.MINUTELY;
import static net.fortuna.ical4j.model.Recur.Frequency.MONTHLY;
import static net.fortuna.ical4j.model.Recur.Frequency.WEEKLY;
import static net.fortuna.ical4j.model.Recur.Frequency.YEARLY;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.repeats.CustomRecurrenceDialog.newCustomRecurrenceDialog;
import static org.tasks.repeats.RecurrenceUtils.newRecur;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import javax.inject.Inject;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.Recur.Frequency;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.ui.SingleCheckedArrayAdapter;
import timber.log.Timber;

@AndroidEntryPoint
public class BasicRecurrenceDialog extends DialogFragment {

  public static final String EXTRA_RRULE = "extra_rrule";
  private static final String EXTRA_DATE = "extra_date";
  private static final String FRAG_TAG_CUSTOM_RECURRENCE = "frag_tag_custom_recurrence";

  @Inject Activity context;
  @Inject DialogBuilder dialogBuilder;
  @Inject RepeatRuleToString repeatRuleToString;

  public static BasicRecurrenceDialog newBasicRecurrenceDialog(
      Fragment target, int rc, String rrule, long dueDate) {
    BasicRecurrenceDialog dialog = new BasicRecurrenceDialog();
    dialog.setTargetFragment(target, rc);
    Bundle arguments = new Bundle();
    if (rrule != null) {
      arguments.putString(EXTRA_RRULE, rrule);
    }
    arguments.putLong(EXTRA_DATE, dueDate);
    dialog.setArguments(arguments);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Bundle arguments = getArguments();
    long dueDate = arguments.getLong(EXTRA_DATE, currentTimeMillis());
    String rule = arguments.getString(EXTRA_RRULE);
    Recur rrule = null;
    try {
      if (!isNullOrEmpty(rule)) {
        rrule = newRecur(rule);
      }
    } catch (Exception e) {
      Timber.e(e);
    }

    boolean customPicked = isCustomValue(rrule);
    List<String> repeatOptions =
        newArrayList(context.getResources().getStringArray(R.array.repeat_options));
    SingleCheckedArrayAdapter adapter =
        new SingleCheckedArrayAdapter(context, repeatOptions);
    int selected = 0;
    if (customPicked) {
      adapter.insert(repeatRuleToString.toString(rule), 0);
    } else if (rrule != null) {
      switch (rrule.getFrequency()) {
        case DAILY:
          selected = 1;
          break;
        case WEEKLY:
          selected = 2;
          break;
        case MONTHLY:
          selected = 3;
          break;
        case YEARLY:
          selected = 4;
          break;
        default:
          selected = 0;
          break;
      }
    }
    return dialogBuilder
        .newDialog()
        .setSingleChoiceItems(
            adapter,
            selected,
            (dialogInterface, i) -> {
              if (customPicked) {
                if (i == 0) {
                  dialogInterface.dismiss();
                  return;
                }
                i--;
              }
              Recur result;
              if (i == 0) {
                result = null;
              } else if (i == 5) {
                newCustomRecurrenceDialog(
                    getTargetFragment(), getTargetRequestCode(), rule, dueDate)
                    .show(getParentFragmentManager(), FRAG_TAG_CUSTOM_RECURRENCE);
                dialogInterface.dismiss();
                return;
              } else {
                result = newRecur();
                result.setInterval(1);

                switch (i) {
                  case 1:
                    result.setFrequency(DAILY.name());
                    break;
                  case 2:
                    result.setFrequency(WEEKLY.name());
                    break;
                  case 3:
                    result.setFrequency(MONTHLY.name());
                    break;
                  case 4:
                    result.setFrequency(YEARLY.name());
                    break;
                }
              }

              Intent intent = new Intent();
              intent.putExtra(EXTRA_RRULE, result == null ? null : result.toString());
              getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, intent);
              dialogInterface.dismiss();
            })
        .setOnCancelListener(null)
        .show();
  }

  private boolean isCustomValue(Recur rrule) {
    if (rrule == null) {
      return false;
    }
    Frequency frequency = rrule.getFrequency();
    return (frequency == WEEKLY || frequency == MONTHLY) && !rrule.getDayList().isEmpty()
        || frequency == HOURLY
        || frequency == MINUTELY
        || rrule.getUntil() != null
        || rrule.getInterval() > 1
        || rrule.getCount() > 0;
  }
}
