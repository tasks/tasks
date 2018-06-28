/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import static android.support.v4.content.ContextCompat.getColor;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_DAY_BEFORE;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_DUE;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_NONE;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_WEEK_BEFORE;
import static java.util.Arrays.asList;
import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeBase;
import org.tasks.time.DateTime;
import org.tasks.ui.HiddenTopArrayAdapter;
import org.tasks.ui.TaskEditControlFragment;

/**
 * Control set for specifying when a task should be hidden
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class HideUntilControlSet extends TaskEditControlFragment implements OnItemSelectedListener {

  public static final int TAG = R.string.TEA_ctrl_hide_until_pref;

  private static final String EXTRA_CUSTOM = "extra_custom";
  private static final String EXTRA_SELECTION = "extra_selection";

  private static final int SPECIFIC_DATE = -1;
  private static final int EXISTING_TIME_UNSET = -2;
  private static final int REQUEST_HIDE_UNTIL = 11011;
  private final List<HideUntilValue> spinnerItems = new ArrayList<>();
  @Inject @ForActivity Context context;
  @Inject ThemeBase themeBase;
  @Inject Preferences preferences;
  // private final CheckBox enabled;
  @BindView(R.id.hideUntil)
  Spinner spinner;

  @BindView(R.id.clear)
  ImageView clearButton;

  private ArrayAdapter<HideUntilValue> adapter;
  private int previousSetting = Task.HIDE_UNTIL_NONE;
  private int selection;
  private long existingDate = EXISTING_TIME_UNSET;
  private HideUntilValue selectedValue;

  @OnClick(R.id.clear)
  void clearHideUntil(View view) {
    updateSpinnerOptions(0);
    selection = 0;
    spinner.setSelection(selection);
    refreshDisplayView();
  }

  @Nullable
  @Override
  public View onCreateView(
      final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    adapter =
        new HiddenTopArrayAdapter<HideUntilValue>(
            context, android.R.layout.simple_spinner_item, spinnerItems) {
          @NonNull
          @Override
          public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            int selectedItemPosition = position;
            if (parent instanceof AdapterView) {
              selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
            }
            TextView tv =
                (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            tv.setPadding(0, 0, 0, 0);
            HideUntilValue value = getItem(selectedItemPosition);
            if (value.setting == Task.HIDE_UNTIL_NONE) {
              clearButton.setVisibility(View.GONE);
              tv.setText(value.labelDisplay);
              tv.setTextColor(getColor(context, R.color.text_tertiary));
            } else {
              String display = value.labelDisplay;
              tv.setText(getString(R.string.TEA_hideUntil_display, display));
              tv.setTextColor(getColor(context, R.color.text_primary));
            }
            return tv;
          }
        };
    if (savedInstanceState == null) {
      long dueDate = task.getDueDate();
      long hideUntil = task.getHideUntil();

      DateTime dueDay =
          newDateTime(dueDate)
              .withHourOfDay(0)
              .withMinuteOfHour(0)
              .withSecondOfMinute(0)
              .withMillisOfSecond(0);

      // For the hide until due case, we need the time component
      long dueTime = dueDate / 1000L * 1000L;

      if (hideUntil <= 0) {
        selection = 0;
        hideUntil = 0;
        if (task.isNew()) {
          int defaultHideUntil =
              preferences.getIntegerFromString(R.string.p_default_hideUntil_key, HIDE_UNTIL_NONE);
          switch (defaultHideUntil) {
            case HIDE_UNTIL_DUE:
              selection = 1;
              break;
            case HIDE_UNTIL_DAY_BEFORE:
              selection = 3;
              break;
            case HIDE_UNTIL_WEEK_BEFORE:
              selection = 4;
              break;
          }
        }
      } else if (hideUntil == dueDay.getMillis()) {
        selection = 1;
        hideUntil = 0;
      } else if (hideUntil == dueTime) {
        selection = 2;
        hideUntil = 0;
      } else if (hideUntil + DateUtilities.ONE_DAY == dueDay.getMillis()) {
        selection = 3;
        hideUntil = 0;
      } else if (hideUntil + DateUtilities.ONE_WEEK == dueDay.getMillis()) {
        selection = 4;
        hideUntil = 0;
      }

      updateSpinnerOptions(hideUntil);
    } else {
      updateSpinnerOptions(savedInstanceState.getLong(EXTRA_CUSTOM));
      selection = savedInstanceState.getInt(EXTRA_SELECTION);
    }
    spinner.setAdapter(adapter);
    spinner.setSelection(selection);
    spinner.setOnItemSelectedListener(this);
    refreshDisplayView();
    return view;
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_hide;
  }

  @Override
  protected int getIcon() {
    return R.drawable.ic_visibility_off_24dp;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_HIDE_UNTIL) {
      if (resultCode == Activity.RESULT_OK) {
        setCustomDate(data.getLongExtra(TimePickerActivity.EXTRA_TIMESTAMP, 0L));
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void apply(Task task) {
    task.setHideUntil(getHideUntil(task));
  }

  @Override
  public boolean hasChanges(Task original) {
    return original.getHideUntil() != getHideUntil(original);
  }

  private long getHideUntil(Task task) {
    return task.createHideUntil(selectedValue.setting, selectedValue.date);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putLong(EXTRA_CUSTOM, existingDate);
    outState.putInt(EXTRA_SELECTION, selection);
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  private void updateSpinnerOptions(long specificDate) {
    spinnerItems.clear();
    // set up base values
    String[] labelsSpinner = getResources().getStringArray(R.array.TEA_hideUntil_spinner);
    String[] labelsDisplay = getResources().getStringArray(R.array.TEA_hideUntil_display);

    spinnerItems.addAll(
        new ArrayList<>(
            asList(
                new HideUntilValue(labelsSpinner[0], labelsDisplay[0], Task.HIDE_UNTIL_DUE),
                new HideUntilValue(labelsSpinner[1], labelsDisplay[1], Task.HIDE_UNTIL_DUE_TIME),
                new HideUntilValue(labelsSpinner[2], labelsDisplay[2], Task.HIDE_UNTIL_DAY_BEFORE),
                new HideUntilValue(labelsSpinner[3], labelsDisplay[3], Task.HIDE_UNTIL_WEEK_BEFORE),
                new HideUntilValue(
                    labelsSpinner[4],
                    "",
                    Task.HIDE_UNTIL_SPECIFIC_DAY,
                    -1)))); // no need for a string for display here, since the chosen day will be
    // displayed

    if (specificDate > 0) {
      spinnerItems.add(0, getHideUntilValue(specificDate));
      existingDate = specificDate;
    } else {
      spinnerItems.add(
          0, new HideUntilValue(getString(R.string.TEA_hideUntil_label), Task.HIDE_UNTIL_NONE));
      existingDate = EXISTING_TIME_UNSET;
    }
    adapter.notifyDataSetChanged();
  }

  private HideUntilValue getHideUntilValue(long timestamp) {
    DateTime hideUntilAsDate = newDateTime(timestamp);
    if (hideUntilAsDate.getHourOfDay() == 0
        && hideUntilAsDate.getMinuteOfHour() == 0
        && hideUntilAsDate.getSecondOfMinute() == 0) {
      return new HideUntilValue(
          DateUtilities.getDateString(newDateTime(timestamp)),
          Task.HIDE_UNTIL_SPECIFIC_DAY,
          timestamp);
    } else {
      return new HideUntilValue(
          DateUtilities.getDateStringWithTime(context, timestamp),
          Task.HIDE_UNTIL_SPECIFIC_DAY_TIME,
          timestamp);
    }
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    // if specific date selected, show dialog
    // ... at conclusion of dialog, update our list
    HideUntilValue item = adapter.getItem(position);
    if (item.date == SPECIFIC_DATE) {
      final DateTime customDate =
          newDateTime(existingDate == EXISTING_TIME_UNSET ? DateUtilities.now() : existingDate)
              .withSecondOfMinute(0);

      final Activity activity = getActivity();
      Intent intent = new Intent(activity, DateAndTimePickerActivity.class);
      intent.putExtra(DateAndTimePickerActivity.EXTRA_TIMESTAMP, customDate.getMillis());
      startActivityForResult(intent, REQUEST_HIDE_UNTIL);
      spinner.setSelection(previousSetting);
    } else {
      previousSetting = position;
    }
    selection = spinner.getSelectedItemPosition();
    refreshDisplayView();
  }

  // --- listening for events

  private void setCustomDate(long timestamp) {
    updateSpinnerOptions(timestamp);
    spinner.setSelection(0);
    refreshDisplayView();
  }

  @Override
  public void onNothingSelected(AdapterView<?> arg0) {
    // ignore
  }

  private void refreshDisplayView() {
    selectedValue = adapter.getItem(selection);
    clearButton.setVisibility(
        selectedValue.setting == Task.HIDE_UNTIL_NONE ? View.GONE : View.VISIBLE);
  }

  // --- setting up values

  /**
   * Container class for urgencies
   *
   * @author Tim Su <tim@todoroo.com>
   */
  private class HideUntilValue {

    final String labelSpinner;
    final String labelDisplay;
    final int setting;
    final long date;

    HideUntilValue(String label, int setting) {
      this(label, label, setting, 0);
    }

    HideUntilValue(String labelSpinner, String labelDisplay, int setting) {
      this(labelSpinner, labelDisplay, setting, 0);
    }

    HideUntilValue(String label, int setting, long date) {
      this(label, label, setting, date);
    }

    HideUntilValue(String labelSpinner, String labelDisplay, int setting, long date) {
      this.labelSpinner = labelSpinner;
      this.labelDisplay = labelDisplay;
      this.setting = setting;
      this.date = date;
    }

    @Override
    public String toString() {
      return labelSpinner;
    }
  }
}
