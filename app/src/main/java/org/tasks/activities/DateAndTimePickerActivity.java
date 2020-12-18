package org.tasks.activities;

import static org.tasks.dialogs.MyDatePickerDialog.newDatePicker;
import static org.tasks.dialogs.MyTimePickerDialog.newTimePicker;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeAccent;
import org.tasks.time.DateTime;

@AndroidEntryPoint
public class DateAndTimePickerActivity extends InjectingAppCompatActivity
    implements MyDatePickerDialog.DatePickerCallback, MyTimePickerDialog.TimePickerCallback {

  public static final String EXTRA_TIMESTAMP = "extra_timestamp";
  private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";
  private static final String FRAG_TAG_TIME_PICKER = "frag_tag_time_picker";
  private static final String EXTRA_DATE_SELECTED = "extra_date_selected";
  @Inject ThemeAccent themeAccent;
  @Inject Preferences preferences;

  private DateTime initial;
  private boolean dateSelected;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    initial = new DateTime(getIntent().getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis()));

    if (savedInstanceState != null) {
      dateSelected = savedInstanceState.getBoolean(EXTRA_DATE_SELECTED, false);
      if (dateSelected) {
        return;
      }
    }

    themeAccent.applyStyle(getTheme());

    androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
      newDatePicker(null, 0, initial.getMillis())
          .show(getSupportFragmentManager(), FRAG_TAG_DATE_PICKER);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(EXTRA_DATE_SELECTED, dateSelected);
  }

  @Override
  public void onDatePicked(DialogInterface dialog, long timestamp) {
    if (timestamp == MyDatePickerDialog.NO_DATE) {
      finish();
    } else {
      dialog.dismiss();
      dateSelected = true;
      newTimePicker(
              null,
              0,
              new DateTime(timestamp).withMillisOfDay(initial.getMillisOfDay()).getMillis())
          .show(getSupportFragmentManager(), FRAG_TAG_TIME_PICKER);
    }
  }

  @Override
  public void onTimePicked(long timestamp) {
    if (timestamp != MyTimePickerDialog.NO_TIME) {
      final Intent data = new Intent();
      data.putExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, timestamp);
      setResult(RESULT_OK, data);
    }
    finish();
  }
}
