package org.tasks.repeats;

import static android.app.Activity.RESULT_OK;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.ical.values.Frequency.DAILY;
import static com.google.ical.values.Frequency.HOURLY;
import static com.google.ical.values.Frequency.MINUTELY;
import static com.google.ical.values.Frequency.MONTHLY;
import static com.google.ical.values.Frequency.WEEKLY;
import static com.google.ical.values.Frequency.YEARLY;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.repeats.CustomRecurrenceDialog.newCustomRecurrenceDialog;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import javax.inject.Inject;
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
      Fragment target, int rc, RRule rrule, long dueDate) {
    BasicRecurrenceDialog dialog = new BasicRecurrenceDialog();
    dialog.setTargetFragment(target, rc);
    Bundle arguments = new Bundle();
    if (rrule != null) {
      arguments.putString(EXTRA_RRULE, rrule.toIcal());
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
    RRule parsed = null;
    try {
      if (!isNullOrEmpty(rule)) {
        parsed = new RRule(rule);
      }
    } catch (Exception e) {
      Timber.e(e);
    }
    RRule rrule = parsed;

    boolean customPicked = isCustomValue(rrule);
    List<String> repeatOptions =
        newArrayList(context.getResources().getStringArray(R.array.repeat_options));
    SingleCheckedArrayAdapter adapter =
        new SingleCheckedArrayAdapter(context, repeatOptions);
    int selected = 0;
    if (customPicked) {
      adapter.insert(repeatRuleToString.toString(rule), 0);
    } else if (rrule != null) {
      switch (rrule.getFreq()) {
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
              RRule result;
              if (i == 0) {
                result = null;
              } else if (i == 5) {
                newCustomRecurrenceDialog(
                    getTargetFragment(), getTargetRequestCode(), rrule, dueDate)
                    .show(getParentFragmentManager(), FRAG_TAG_CUSTOM_RECURRENCE);
                dialogInterface.dismiss();
                return;
              } else {
                result = new RRule();
                result.setInterval(1);

                switch (i) {
                  case 1:
                    result.setFreq(DAILY);
                    break;
                  case 2:
                    result.setFreq(WEEKLY);
                    break;
                  case 3:
                    result.setFreq(MONTHLY);
                    break;
                  case 4:
                    result.setFreq(YEARLY);
                    break;
                }
              }

              Intent intent = new Intent();
              intent.putExtra(EXTRA_RRULE, result == null ? null : result.toIcal());
              getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, intent);
              dialogInterface.dismiss();
            })
        .setOnCancelListener(null)
        .show();
  }

  private boolean isCustomValue(RRule rrule) {
    if (rrule == null) {
      return false;
    }
    Frequency frequency = rrule.getFreq();
    return (frequency == WEEKLY || frequency == MONTHLY) && !rrule.getByDay().isEmpty()
        || frequency == HOURLY
        || frequency == MINUTELY
        || rrule.getUntil() != null
        || rrule.getInterval() != 1
        || rrule.getCount() != 0;
  }
}
