package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

public class AppearancePreferences extends InjectingPreferenceActivity {

    public static String FORCE_REFRESH = "force_refresh";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_appearance);

        setRefreshOnChange(R.string.p_use_dark_theme);
        setRefreshOnChange(R.string.p_fontSize);
        setRefreshOnChange(R.string.p_fullTaskTitle);
    }

    private void setRefreshOnChange(int resId) {
        findPreference(getString(resId)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setResult(RESULT_OK, new Intent().putExtra(FORCE_REFRESH, true));
                return true;
            }
        });
    }
}
