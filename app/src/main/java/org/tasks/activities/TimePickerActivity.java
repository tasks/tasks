package org.tasks.activities;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import javax.inject.Inject;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.time.DateTime;

public class TimePickerActivity extends InjectingAppCompatActivity
    implements TimePickerDialog.OnTimeSetListener {

  public static final String EXTRA_TIMESTAMP = "extra_timestamp";
  private static final String FRAG_TAG_TIME_PICKER = "frag_tag_time_picker";
  @Inject ThemeBase themeBase;
  @Inject ThemeAccent themeAccent;

  private DateTime initial;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    initial = new DateTime(getIntent().getLongExtra(EXTRA_TIMESTAMP, currentTimeMillis()));

    androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
    MyTimePickerDialog dialog =
        (MyTimePickerDialog) fragmentManager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
    if (dialog == null) {
      dialog = new MyTimePickerDialog();
      dialog.initialize(
          null,
          initial.getHourOfDay(),
          initial.getMinuteOfHour(),
          0,
          DateFormat.is24HourFormat(this));
      dialog.setThemeDark(themeBase.isDarkTheme(this));
      dialog.setAccentColor(themeAccent.getAccentColor());
      dialog.show(fragmentManager, FRAG_TAG_TIME_PICKER);
    }
    dialog.setOnCancelListener(dialogInterface -> finish());
    dialog.setOnTimeSetListener(this);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void onTimeSet(
      TimePickerDialog timePickerDialog, final int hours, final int minutes, int seconds) {
    Intent data = new Intent();
    data.putExtra(
        EXTRA_TIMESTAMP,
        initial.startOfDay().withHourOfDay(hours).withMinuteOfHour(minutes).getMillis());
    setResult(RESULT_OK, data);
    finish();
  }
}
