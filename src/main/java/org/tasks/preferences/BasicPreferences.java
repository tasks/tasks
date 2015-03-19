package org.tasks.preferences;

import android.os.Bundle;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

public class BasicPreferences extends InjectingPreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action == null) {
            addPreferencesFromResource(R.xml.preferences);
        } else if (action.equals(getString(R.string.EPr_appearance_header))) {
            toolbar.setTitle(getString(R.string.EPr_appearance_header));
            addPreferencesFromResource(R.xml.preferences_appearance);
        } else if (action.equals(getString(R.string.backup_BPr_header))) {
            toolbar.setTitle(getString(R.string.backup_BPr_header));
            addPreferencesFromResource(R.xml.preferences_backup);
        }
    }
}
