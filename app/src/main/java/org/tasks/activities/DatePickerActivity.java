package org.tasks.activities;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import javax.inject.Inject;
import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.time.DateTime;

public class DatePickerActivity extends InjectingAppCompatActivity
    implements DatePickerDialog.OnDateSetListener {

  public static final String EXTRA_TIMESTAMP = "extra_timestamp";
  private static final String FRAG_TAG_DATE_PICKER = "frag_tag_date_picker";
  @Inject ThemeBase themeBase;
  @Inject ThemeAccent themeAccent;
  @Inject Preferences preferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    long timestamp = getIntent().getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis());
    DateTime initial = (timestamp > 0 ? new DateTime(timestamp) : new DateTime()).startOfDay();

    FragmentManager fragmentManager = getSupportFragmentManager();
    MyDatePickerDialog dialog =
        (MyDatePickerDialog) fragmentManager.findFragmentByTag(FRAG_TAG_DATE_PICKER);
    if (dialog == null) {
      dialog = new MyDatePickerDialog();
      dialog.initialize(
          null, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth());
      dialog.setVersion(DatePickerDialog.Version.VERSION_2);
      dialog.setThemeDark(themeBase.isDarkTheme(this));
      dialog.setAccentColor(themeAccent.getAccentColor());
      int firstDayOfWeek = preferences.getFirstDayOfWeek();
      if (firstDayOfWeek >= 1 && firstDayOfWeek <= 7) {
        dialog.setFirstDayOfWeek(firstDayOfWeek);
      }
      dialog.show(fragmentManager, FRAG_TAG_DATE_PICKER);
    }
    dialog.setOnCancelListener(dialogInterface -> finish());
    dialog.setOnDateSetListener(this);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void onDateSet(
      DatePickerDialog view, final int year, final int monthOfYear, final int dayOfMonth) {
    Intent data = new Intent();
    data.putExtra(EXTRA_TIMESTAMP, new DateTime(year, monthOfYear + 1, dayOfMonth).getMillis());
    setResult(RESULT_OK, data);
    finish();
  }
}
