package org.tasks.preferences;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

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
import org.tasks.locale.Locale;
import org.tasks.locale.LocalePickerDialog;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static org.tasks.dialogs.ThemePickerDialog.newThemePickerDialog;
import static org.tasks.locale.LocalePickerDialog.newLocalePickerDialog;

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
    @Inject Locale locale;
    @Inject ThemeCache themeCache;

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
        findPreference(getString(R.string.p_layout_direction)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                tracker.reportEvent(Tracking.Events.SET_PREFERENCE, R.string.p_layout_direction, o.toString());
                int newValue = Integer.parseInt((String) o);
                if (locale.getDirectionality() != locale.withDirectionality(newValue).getDirectionality()) {
                    showRestartDialog();
                }
                return true;
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

        requires(R.string.settings_localization, atLeastJellybeanMR1(), R.string.p_language, R.string.p_layout_direction);
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
                themeCache.getThemeBase(index).setDefaultNightMode();
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
    public void onLocaleSelected(Locale newValue) {
        String override = newValue.getLanguageOverride();
        if (Strings.isNullOrEmpty(override)) {
            preferences.remove(R.string.p_language);
        } else {
            preferences.setString(R.string.p_language, override);
            tracker.reportEvent(Tracking.Events.SET_PREFERENCE, R.string.p_language, override);
        }
        updateLocale();
        if (!locale.equals(newValue)) {
            showRestartDialog();
        }
    }

    private void showRestartDialog() {
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

    private void updateLocale() {
        Preference languagePreference = findPreference(getString(R.string.p_language));
        String preference = preferences.getStringValue(R.string.p_language);
        languagePreference.setSummary(locale.withLanguage(preference).getDisplayName());
    }

    @Override
    public void finish() {
        setResult(Activity.RESULT_OK, new Intent() {{
            putExtras(result);
        }});

        super.finish();
    }
}
