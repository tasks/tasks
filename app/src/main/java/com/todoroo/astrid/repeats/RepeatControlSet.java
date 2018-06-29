/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.ical.values.Frequency.MONTHLY;
import static org.tasks.repeats.BasicRecurrenceDialog.newBasicRecurrenceDialog;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import com.google.common.base.Strings;
import com.google.ical.values.RRule;
import com.google.ical.values.WeekdayNum;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.data.Task;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.repeats.RepeatRuleToString;
import org.tasks.themes.Theme;
import org.tasks.time.DateTime;
import org.tasks.ui.HiddenTopArrayAdapter;
import org.tasks.ui.TaskEditControlFragment;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class RepeatControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_repeat_pref;
  private static final int TYPE_DUE_DATE = 1;
  private static final int TYPE_COMPLETION_DATE = 2;
  private static final String FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence";
  private static final String EXTRA_RECURRENCE = "extra_recurrence";
  private static final String EXTRA_DUE_DATE = "extra_due_date";
  private static final String EXTRA_REPEAT_AFTER_COMPLETION = "extra_repeat_after_completion";
  private final List<String> repeatTypes = new ArrayList<>();
  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
  @Inject Theme theme;
  @Inject Tracker tracker;
  @Inject RepeatRuleToString repeatRuleToString;

  @BindView(R.id.display_row_edit)
  TextView displayView;

  @BindView(R.id.repeatType)
  Spinner typeSpinner;

  @BindView(R.id.repeatTypeContainer)
  LinearLayout repeatTypeContainer;

  private RRule rrule;
  private HiddenTopArrayAdapter<String> typeAdapter;
  private long dueDate;
  private boolean repeatAfterCompletion;

  public void onSelected(RRule rrule) {
    this.rrule = rrule;
    refreshDisplayView();
  }

  public void onDueDateChanged(long dueDate) {
    this.dueDate = dueDate > 0 ? dueDate : currentTimeMillis();
    if (rrule != null && rrule.getFreq() == MONTHLY && !rrule.getByDay().isEmpty()) {
      WeekdayNum weekdayNum = rrule.getByDay().get(0);
      DateTime dateTime = new DateTime(this.dueDate);
      int num;
      int dayOfWeekInMonth = dateTime.getDayOfWeekInMonth();
      if (weekdayNum.num == -1 || dayOfWeekInMonth == 5) {
        num = dayOfWeekInMonth == dateTime.getMaxDayOfWeekInMonth() ? -1 : dayOfWeekInMonth;
      } else {
        num = dayOfWeekInMonth;
      }
      rrule.setByDay(newArrayList(new WeekdayNum(num, dateTime.getWeekday())));
      refreshDisplayView();
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (savedInstanceState == null) {
      repeatAfterCompletion = task.repeatAfterCompletion();
      dueDate = task.getDueDate();
      if (dueDate <= 0) {
        dueDate = currentTimeMillis();
      }
      try {
        rrule = new RRule(task.getRecurrenceWithoutFrom());
        rrule.setUntil(new DateTime(task.getRepeatUntil()).toDateValue());
      } catch (ParseException e) {
        rrule = null;
      }
    } else {
      String recurrence = savedInstanceState.getString(EXTRA_RECURRENCE);
      dueDate = savedInstanceState.getLong(EXTRA_DUE_DATE);
      if (Strings.isNullOrEmpty(recurrence)) {
        rrule = null;
      } else {
        try {
          rrule = new RRule(recurrence);
        } catch (ParseException e) {
          rrule = null;
        }
      }
      repeatAfterCompletion = savedInstanceState.getBoolean(EXTRA_REPEAT_AFTER_COMPLETION);
    }

    repeatTypes.add("");
    repeatTypes.addAll(Arrays.asList(getResources().getStringArray(R.array.repeat_type)));
    typeAdapter =
        new HiddenTopArrayAdapter<String>(context, 0, repeatTypes) {
          @NonNull
          @Override
          public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            int selectedItemPosition = position;
            if (parent instanceof AdapterView) {
              selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
            }
            TextView tv =
                (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            tv.setPadding(0, 0, 0, 0);
            tv.setText(repeatTypes.get(selectedItemPosition));
            return tv;
          }
        };
    Drawable drawable =
        DrawableCompat.wrap(
            ContextCompat.getDrawable(context, R.drawable.textfield_underline_black));
    drawable.mutate();
    DrawableCompat.setTint(drawable, getColor(context, R.color.text_primary));
    typeSpinner.setBackgroundDrawable(drawable);
    typeSpinner.setAdapter(typeAdapter);
    typeSpinner.setSelection(repeatAfterCompletion ? TYPE_COMPLETION_DATE : TYPE_DUE_DATE);

    refreshDisplayView();
    return view;
  }

  @OnItemSelected(R.id.repeatType)
  public void onRepeatTypeChanged(int position) {
    repeatAfterCompletion = position == TYPE_COMPLETION_DATE;
    repeatTypes.set(0, repeatAfterCompletion ? repeatTypes.get(2) : repeatTypes.get(1));
    typeAdapter.notifyDataSetChanged();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_RECURRENCE, rrule == null ? "" : rrule.toIcal());
    outState.putBoolean(EXTRA_REPEAT_AFTER_COMPLETION, repeatAfterCompletion);
    outState.putLong(EXTRA_DUE_DATE, dueDate);
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @OnClick(R.id.display_row_edit)
  void openPopup(View view) {
    newBasicRecurrenceDialog(this, rrule, dueDate)
        .show(getFragmentManager(), FRAG_TAG_BASIC_RECURRENCE);
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_repeat_display;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_repeat_24dp;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public boolean hasChanges(Task original) {
    return !getRecurrenceValue().equals(original.getRecurrence())
        || original.getRepeatUntil()
            != (rrule == null ? 0 : DateTime.from(rrule.getUntil()).getMillis());
  }

  @Override
  public void apply(Task task) {
    task.setRepeatUntil(rrule == null ? 0 : DateTime.from(rrule.getUntil()).getMillis());
    task.setRecurrence(getRecurrenceValue());
  }

  private String getRecurrenceValue() {
    if (rrule == null) {
      return "";
    }
    RRule copy;
    try {
      copy = new RRule(rrule.toIcal());
    } catch (ParseException e) {
      return "";
    }
    copy.setUntil(null);
    String result = copy.toIcal();
    if (repeatAfterCompletion && !TextUtils.isEmpty(result)) {
      result += ";FROM=COMPLETION"; // $NON-NLS-1$
    }

    return result;
  }

  private void refreshDisplayView() {
    if (rrule == null) {
      displayView.setText(null);
      repeatTypeContainer.setVisibility(View.GONE);
    } else {
      displayView.setText(repeatRuleToString.toString(rrule));
      repeatTypeContainer.setVisibility(View.VISIBLE);
    }
    TaskEditFragment targetFragment = (TaskEditFragment) getParentFragment();
    if (targetFragment != null) {
      targetFragment.onRepeatChanged(rrule != null);
    }
  }
}
