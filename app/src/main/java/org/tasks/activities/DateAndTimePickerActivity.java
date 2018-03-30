package org.tasks.activities;

import static org.tasks.dialogs.NativeDatePickerDialog.newNativeDatePickerDialog;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.dialogs.NativeDatePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.time.DateTime;

public class DateAndTimePickerActivity extends InjectingAppCompatActivity
    implements DatePickerDialog.OnDateSetListener,
        DialogInterface.OnCancelListener,
        NativeDatePickerDialog.NativeDatePickerDialogCallback {

  public static final String EXTRA_TIMESTAMP = "extra_timestamp";
  private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";
  private static final String EXTRA_DATE_SELECTED = "extra_date_selected";
  @Inject ThemeBase themeBase;
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

    FragmentManager fragmentManager = getFragmentManager();
    if (preferences.getBoolean(R.string.p_use_native_datetime_pickers, false)) {
      if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER) == null) {
        newNativeDatePickerDialog(initial).show(fragmentManager, FRAG_TAG_DATE_PICKER);
      }
    } else {
      MyDatePickerDialog datePickerDialog =
          (MyDatePickerDialog) fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER);
      if (datePickerDialog == null) {
        datePickerDialog = new MyDatePickerDialog();
        datePickerDialog.initialize(
            null, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth());
        datePickerDialog.setThemeDark(themeBase.isDarkTheme(this));
        datePickerDialog.setAccentColor(themeAccent.getAccentColor());
        int firstDayOfWeek = preferences.getFirstDayOfWeek();
        if (firstDayOfWeek >= 1 && firstDayOfWeek <= 7) {
          datePickerDialog.setFirstDayOfWeek(firstDayOfWeek);
        }
        datePickerDialog.show(fragmentManager, FRAG_TAG_DATE_PICKER);
      }
      datePickerDialog.setOnCancelListener(this);
      datePickerDialog.setOnDateSetListener(this);
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(EXTRA_DATE_SELECTED, dateSelected);
  }

  @Override
  public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
    datePickerDialog.dismiss();
    dateSet(year, month, day);
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    finish();
  }

  @Override
  public void cancel() {
    finish();
  }

  @Override
  public void onDateSet(int year, int month, int day) {
    dateSet(year, month, day);
  }

  private void dateSet(int year, int month, int day) {
    dateSelected = true;
    final long timestamp =
        new DateTime(year, month + 1, day).withMillisOfDay(initial.getMillisOfDay()).getMillis();
    Intent intent = new Intent(this, TimePickerActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
    intent.putExtra(TimePickerActivity.EXTRA_TIMESTAMP, timestamp);
    startActivity(intent);
    finish();
  }
}
