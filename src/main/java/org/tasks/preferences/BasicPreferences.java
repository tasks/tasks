package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

public class BasicPreferences extends InjectingPreferenceActivity {

    private static final int RC_PREFS = 10001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action == null) {
            addPreferencesFromResource(R.xml.preferences);
            if (!getResources().getBoolean(R.bool.sync_enabled)) {
                getPreferenceScreen().removePreference(findPreference(getString(R.string.synchronization)));
            }
            findPreference(getString(R.string.EPr_appearance_header)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivityForResult(new Intent(BasicPreferences.this, AppearancePreferences.class), RC_PREFS);
                    return true;
                }
            });
        } else if (action.equals(getString(R.string.EPr_appearance_header))) {
            toolbar.setTitle(getString(R.string.EPr_appearance_header));
            addPreferencesFromResource(R.xml.preferences_appearance);
        } else if (action.equals(getString(R.string.backup_BPr_header))) {
            toolbar.setTitle(getString(R.string.backup_BPr_header));
            addPreferencesFromResource(R.xml.preferences_backup);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PREFS) {
            setResult(resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
