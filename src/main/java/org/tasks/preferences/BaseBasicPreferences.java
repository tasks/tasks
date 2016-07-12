package org.tasks.preferences;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;

import com.google.common.base.Strings;
import com.jakewharton.processphoenix.ProcessPhoenix;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.reminders.ReminderPreferences;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.locale.LocalePickerDialog;
import org.tasks.locale.LocaleUtils;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeColor;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static org.tasks.dialogs.ThemePickerDialog.newThemePickerDialog;
import static org.tasks.locale.LocalePickerDialog.newLocalePickerDialog;
import static org.tasks.locale.LocaleUtils.localeFromString;

public abstract class BaseBasicPreferences extends InjectingPreferenceActivity implements
        ThemePickerDialog.ThemePickerCallback,
        LocalePickerDialog.LocaleSelectionHandler {

    private static final String EXTRA_RESULT = "extra_result";
    private static final String FRAG_TAG_THEME_PICKER = "frag_tag_theme_picker";
    private static final String FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker";
    private static final String FRAG_TAG_ACCENT_PICKER = "frag_tag_accent_picker";
    private static final String FRAG_TAG_LOCALE_PICKER = "frag_tag_locale_picker";
    private static final int RC_PREFS = 10001;

    @Inject Tracker tracker;
    @Inject Preferences preferences;
    @Inject ThemeBase themeBase;
    @Inject ThemeColor themeColor;
    @Inject ThemeAccent themeAccent;
    @Inject DialogBuilder dialogBuilder;
    private Bundle result;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_RESULT);

        addPreferencesFromResource(R.xml.preferences);
        addPreferencesFromResource(R.xml.preferences_addons);
        addPreferencesFromResource(R.xml.preferences_privacy);

        Preference themePreference = findPreference(getString(R.string.p_theme));
        themePreference.setSummary(themeBase.getName());
        themePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager.findFragmentByTag(FRAG_TAG_THEME_PICKER) == null) {
                    newThemePickerDialog(ThemePickerDialog.ColorPalette.THEMES)
                            .show(fragmentManager, FRAG_TAG_THEME_PICKER);
                }
                return false;
            }
        });
        Preference colorPreference = findPreference(getString(R.string.p_theme_color));
        colorPreference.setSummary(themeColor.getName());
        colorPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager.findFragmentByTag(FRAG_TAG_THEME_PICKER) == null) {
                    newThemePickerDialog(ThemePickerDialog.ColorPalette.COLORS)
                            .show(fragmentManager, FRAG_TAG_COLOR_PICKER);
                }
                return false;
            }
        });
        Preference accentPreference = findPreference(getString(R.string.p_theme_accent));
        accentPreference.setSummary(themeAccent.getName());
        accentPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager.findFragmentByTag(FRAG_TAG_ACCENT_PICKER) == null) {
                    newThemePickerDialog(ThemePickerDialog.ColorPalette.ACCENTS)
                            .show(fragmentManager, FRAG_TAG_ACCENT_PICKER);
                }
                return false;
            }
        });
        Preference languagePreference = findPreference(getString(R.string.p_language));
        updateLocale();
        languagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                newLocalePickerDialog()
                        .show(getFragmentManager(), FRAG_TAG_LOCALE_PICKER);
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

        requires(R.string.settings_general, atLeastJellybeanMR1(), R.string.p_language);
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
    public void themePicked(ThemePickerDialog.ColorPalette palette, int index) {
        switch (palette) {
            case THEMES:
                preferences.setInt(R.string.p_theme, index);
                tracker.reportEvent(Tracking.Events.SET_THEME, Integer.toString(index));
                break;
            case COLORS:
                preferences.setInt(R.string.p_theme_color, index);
                tracker.reportEvent(Tracking.Events.SET_COLOR, Integer.toString(index));
                break;
            case ACCENTS:
                preferences.setInt(R.string.p_theme_accent, index);
                tracker.reportEvent(Tracking.Events.SET_ACCENT, Integer.toString(index));
                break;
        }
        result.putBoolean(AppearancePreferences.EXTRA_RESTART, true);
        recreate();
    }

    @Override
    public void onLocaleSelected(String newValue) {
        if (newValue == null) {
            preferences.remove(R.string.p_language);
        } else {
            preferences.setString(R.string.p_language, newValue);
        }
        tracker.reportEvent(Tracking.Events.SET_PREFERENCE, R.string.p_language, newValue);
        updateLocale();
        String currentValue = LocaleUtils.getsLocaleString();
        if (!TextUtils.equals(currentValue, newValue)) {
            dialogBuilder.newDialog()
                    .setMessage(R.string.restart_required)
                    .setPositiveButton(R.string.restart_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ProcessPhoenix.triggerRebirth(BaseBasicPreferences.this, new Intent(BaseBasicPreferences.this, TaskListActivity.class) {{
                                putExtra(TaskListActivity.OPEN_FILTER, (Filter) null);
                            }});
                        }
                    })
                    .setNegativeButton(R.string.restart_later, null)
                    .show();
        }
    }

    private void updateLocale() {
        Preference languagePreference = findPreference(getString(R.string.p_language));
        String locale = preferences.getStringValue(R.string.p_language);
        languagePreference.setSummary(Strings.isNullOrEmpty(locale)
                ? getString(R.string.default_value)
                : localeFromString(locale).getDisplayName());
    }

    @Override
    public void finish() {
        setResult(Activity.RESULT_OK, new Intent() {{
            putExtras(result);
        }});

        super.finish();
    }
}
