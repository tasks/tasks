package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;

import org.tasks.R;

public class AppearancePreferences extends TodorooPreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // first-order preferences

        findPreference(getString(R.string.p_beastMode)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                startActivity(new Intent(AppearancePreferences.this, BeastModePreferences.class) {{
                    setAction(AstridApiConstants.ACTION_SETTINGS);
                }});
                return true;
            }
        });
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_appearance;
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
    }
}
