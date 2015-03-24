package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

public class AppearancePreferences extends InjectingPreferenceActivity {

    public static String FORCE_REFRESH = "force_refresh";
    public static String FILTERS_CHANGED = "filters_changed";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_appearance);

        setExtraOnChange(R.string.p_use_dark_theme, FORCE_REFRESH);
        setExtraOnChange(R.string.p_fontSize, FORCE_REFRESH);
        setExtraOnChange(R.string.p_fullTaskTitle, FORCE_REFRESH);
        setExtraOnChange(R.string.p_show_today_filter, FILTERS_CHANGED);
        setExtraOnChange(R.string.p_show_recently_modified_filter, FILTERS_CHANGED);
        setExtraOnChange(R.string.p_show_not_in_list_filter, FILTERS_CHANGED);
    }

    private void setExtraOnChange(int resId, final String extra) {
        findPreference(getString(resId)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setResult(RESULT_OK, new Intent().putExtra(extra, true));
                return true;
            }
        });
    }
}
