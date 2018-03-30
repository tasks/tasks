package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.widget.Toast;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.TimePickerActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.locale.Locale;
import org.tasks.time.DateTime;
import org.tasks.ui.TimePreference;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.format.TextStyle;

public class DateTimePreferences extends InjectingPreferenceActivity
    implements Preference.OnPreferenceChangeListener {

  private static final int REQUEST_MORNING = 10001;
  private static final int REQUEST_AFTERNOON = 10002;
  private static final int REQUEST_EVENING = 10003;
  private static final int REQUEST_NIGHT = 10004;

  @Inject Locale locale;
  @Inject Preferences preferences;
  @Inject Tracker tracker;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_date_time);

    ListPreference startOfWeekPreference = getStartOfWeekPreference();
    startOfWeekPreference.setEntries(getWeekdayEntries());
    startOfWeekPreference.setOnPreferenceChangeListener(this);

    initializeTimePreference(getMorningPreference(), REQUEST_MORNING);
    initializeTimePreference(getAfternoonPreference(), REQUEST_AFTERNOON);
    initializeTimePreference(getEveningPreference(), REQUEST_EVENING);
    initializeTimePreference(getNightPreference(), REQUEST_NIGHT);

    updateStartOfWeek(preferences.getStringValue(R.string.p_start_of_week));
  }

  private String[] getWeekdayEntries() {
    return new String[] {
      getString(R.string.use_locale_default),
      getWeekdayDisplayName(DayOfWeek.SUNDAY),
      getWeekdayDisplayName(DayOfWeek.MONDAY)
    };
  }

  private String getWeekdayDisplayName(DayOfWeek dayOfWeek) {
    return dayOfWeek.getDisplayName(TextStyle.FULL, locale.getLocale());
  }

  private TimePreference getMorningPreference() {
    return getTimePreference(R.string.p_date_shortcut_morning);
  }

  private TimePreference getAfternoonPreference() {
    return getTimePreference(R.string.p_date_shortcut_afternoon);
  }

  private TimePreference getEveningPreference() {
    return getTimePreference(R.string.p_date_shortcut_evening);
  }

  private TimePreference getNightPreference() {
    return getTimePreference(R.string.p_date_shortcut_night);
  }

  private TimePreference getTimePreference(int resId) {
    return (TimePreference) findPreference(getString(resId));
  }

  private void initializeTimePreference(final TimePreference preference, final int requestCode) {
    preference.setOnPreferenceChangeListener(this);
    preference.setOnPreferenceClickListener(
        ignored -> {
          final DateTime current = new DateTime().withMillisOfDay(preference.getMillisOfDay());
          Intent intent = new Intent(DateTimePreferences.this, TimePickerActivity.class);
          intent.putExtra(TimePickerActivity.EXTRA_TIMESTAMP, current.getMillis());
          startActivityForResult(intent, requestCode);
          return true;
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        case REQUEST_MORNING:
          getMorningPreference().handleTimePickerActivityIntent(data);
          return;
        case REQUEST_AFTERNOON:
          getAfternoonPreference().handleTimePickerActivityIntent(data);
          return;
        case REQUEST_EVENING:
          getEveningPreference().handleTimePickerActivityIntent(data);
          return;
        case REQUEST_NIGHT:
          getNightPreference().handleTimePickerActivityIntent(data);
          return;
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue) {
    if (preference.equals(getStartOfWeekPreference())) {
      updateStartOfWeek(newValue.toString());
    } else {
      int millisOfDay = (int) newValue;

      if (preference.equals(getMorningPreference())) {
        if (millisOfDay >= getAfternoonPreference().getMillisOfDay()) {
          mustComeBefore(R.string.date_shortcut_morning, R.string.date_shortcut_afternoon);
          return false;
        }
      } else if (preference.equals(getAfternoonPreference())) {
        if (millisOfDay <= getMorningPreference().getMillisOfDay()) {
          mustComeAfter(R.string.date_shortcut_afternoon, R.string.date_shortcut_morning);
          return false;
        } else if (millisOfDay >= getEveningPreference().getMillisOfDay()) {
          mustComeBefore(R.string.date_shortcut_afternoon, R.string.date_shortcut_evening);
          return false;
        }
      } else if (preference.equals(getEveningPreference())) {
        if (millisOfDay <= getAfternoonPreference().getMillisOfDay()) {
          mustComeAfter(R.string.date_shortcut_evening, R.string.date_shortcut_afternoon);
          return false;
        } else if (millisOfDay >= getNightPreference().getMillisOfDay()) {
          mustComeBefore(R.string.date_shortcut_evening, R.string.date_shortcut_night);
          return false;
        }
      } else if (preference.equals(getNightPreference())) {
        if (millisOfDay <= getEveningPreference().getMillisOfDay()) {
          mustComeAfter(R.string.date_shortcut_night, R.string.date_shortcut_evening);
          return false;
        }
      }
    }

    tracker.reportEvent(Tracking.Events.SET_PREFERENCE, preference.getKey(), newValue.toString());
    return true;
  }

  private void mustComeBefore(int settingResId, int relativeResId) {
    invalidSetting(R.string.date_shortcut_must_come_before, settingResId, relativeResId);
  }

  private void mustComeAfter(int settingResId, int relativeResId) {
    invalidSetting(R.string.date_shortcut_must_come_after, settingResId, relativeResId);
  }

  private void invalidSetting(int errorResId, int settingResId, int relativeResId) {
    Toast.makeText(
            this,
            getString(errorResId, getString(settingResId), getString(relativeResId)),
            Toast.LENGTH_SHORT)
        .show();
  }

  private void updateStartOfWeek(String value) {
    ListPreference preference = getStartOfWeekPreference();
    int index = preference.findIndexOfValue(value);
    String summary = getWeekdayEntries()[index];
    preference.setSummary(summary);
  }

  private ListPreference getStartOfWeekPreference() {
    return (ListPreference) findPreference(R.string.p_start_of_week);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
