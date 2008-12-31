package com.timsu.astrid.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.timsu.astrid.R;

public class EditPreferences extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}