package org.tasks.preferences;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.tasks.R;

public class AppearancePreferences extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_appearance);
    }
}
