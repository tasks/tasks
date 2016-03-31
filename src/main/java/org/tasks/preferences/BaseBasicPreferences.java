package org.tasks.preferences;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.reminders.ReminderPreferences;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

public abstract class BaseBasicPreferences extends InjectingPreferenceActivity implements ThemePickerDialog.ThemePickerCallback {

    private static final String EXTRA_RESULT = "extra_result";
    private static final String FRAG_TAG_THEME_PICKER = "frag_tag_theme_picker";
    private static final int RC_PREFS = 10001;

    @Inject Tracker tracker;
    @Inject ThemeManager themeManager;
    @Inject Preferences preferences;
    private Bundle result;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_RESULT);

        addPreferencesFromResource(R.xml.preferences);
        addPreferencesFromResource(R.xml.preferences_addons);
        addPreferencesFromResource(R.xml.preferences_privacy);

        Preference themePreference = findPreference(getString(R.string.p_theme));
        themePreference.setSummary(themeManager.getAppTheme().getName());
        themePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager.findFragmentByTag(FRAG_TAG_THEME_PICKER) == null) {
                    new ThemePickerDialog()
                            .show(fragmentManager, FRAG_TAG_THEME_PICKER);
                }
                return false;
            }
        });

        findPreference(getString(R.string.p_collect_statistics)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    tracker.setTrackingEnabled((boolean) newValue);
                    return true;
                }
                return false;
            }
        });

        setupActivity(R.string.EPr_appearance_header, AppearancePreferences.class);
        setupActivity(R.string.notifications, ReminderPreferences.class);
        setupActivity(R.string.EPr_manage_header, OldTaskPreferences.class);
    }

    private void setupActivity(int key, final Class<?> target) {
        findPreference(getString(key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(BaseBasicPreferences.this, target), RC_PREFS);
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(EXTRA_RESULT, result);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PREFS) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                result.putAll(data.getExtras());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void themePicked(Theme theme) {
        int index = theme.getThemeIndex();
        preferences.setInt(R.string.p_theme, index);
        tracker.reportEvent(Tracking.Events.SET_THEME, Integer.toString(index));
        result.putBoolean(AppearancePreferences.EXTRA_RESTART, true);
        recreate();
    }

    @Override
    public void finish() {
        setResult(Activity.RESULT_OK, new Intent() {{
            putExtras(result);
        }});

        super.finish();
    }
}
