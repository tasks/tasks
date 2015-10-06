package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import org.tasks.time.DateTime;
import org.tasks.R;
import org.tasks.activities.TimePickerActivity;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.ui.TimePreference;

public class DateShortcutPreferences extends InjectingPreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final int REQUEST_MORNING = 10001;
    private static final int REQUEST_AFTERNOON = 10002;
    private static final int REQUEST_EVENING = 10003;
    private static final int REQUEST_NIGHT = 10004;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_date_shortcuts);

        initializeTimePreference(getMorningPreference(), REQUEST_MORNING);
        initializeTimePreference(getAfternoonPreference(), REQUEST_AFTERNOON);
        initializeTimePreference(getEveningPreference(), REQUEST_EVENING);
        initializeTimePreference(getNightPreference(), REQUEST_NIGHT);
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
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference ignored) {
                final DateTime current = new DateTime().withMillisOfDay(preference.getMillisOfDay());
                startActivityForResult(new Intent(DateShortcutPreferences.this, TimePickerActivity.class) {{
                    putExtra(TimePickerActivity.EXTRA_TIMESTAMP, current.getMillis());
                }}, requestCode);
                return true;
            }
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
        int millisOfDay = (int) newValue;

        if (preference.equals(getMorningPreference())) {
            if (millisOfDay >= getAfternoonPreference().getMillisOfDay()) {
                mustComeBefore(R.string.date_shortcut_morning, R.string.date_shortcut_afternoon);
            } else {
                return true;
            }
        } else if (preference.equals(getAfternoonPreference())) {
            if (millisOfDay <= getMorningPreference().getMillisOfDay()) {
                mustComeAfter(R.string.date_shortcut_afternoon, R.string.date_shortcut_morning);
            } else if (millisOfDay >= getEveningPreference().getMillisOfDay()) {
                mustComeBefore(R.string.date_shortcut_afternoon, R.string.date_shortcut_evening);
            } else {
                return true;
            }
        } else if (preference.equals(getEveningPreference())) {
            if (millisOfDay <= getAfternoonPreference().getMillisOfDay()) {
                mustComeAfter(R.string.date_shortcut_evening, R.string.date_shortcut_afternoon);
            } else if (millisOfDay >= getNightPreference().getMillisOfDay()) {
                mustComeBefore(R.string.date_shortcut_evening, R.string.date_shortcut_night);
            } else {
                return true;
            }
        } else if (preference.equals(getNightPreference())) {
            if (millisOfDay <= getEveningPreference().getMillisOfDay()) {
                mustComeAfter(R.string.date_shortcut_night, R.string.date_shortcut_evening);
            } else {
                return true;
            }
        }
        return false;
    }

    private void mustComeBefore(int settingResId, int relativeResId) {
        invalidSetting(R.string.date_shortcut_must_come_before, settingResId, relativeResId);
    }

    private void mustComeAfter(int settingResId, int relativeResId) {
        invalidSetting(R.string.date_shortcut_must_come_after, settingResId, relativeResId);
    }

    private void invalidSetting(int errorResId, int settingResId, int relativeResId) {
        Toast.makeText(this,
                getString(errorResId, getString(settingResId), getString(relativeResId)),
                Toast.LENGTH_SHORT)
                .show();
    }
}
