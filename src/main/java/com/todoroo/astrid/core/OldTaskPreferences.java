/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.service.StartupService;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class OldTaskPreferences extends InjectingPreferenceActivity {

    private static final String EXTRA_RESULT = "extra_result";

    public static String TOGGLE_DELETED = "toggle_deleted";

    private Bundle result;

    @Inject StartupService startupService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_RESULT);

        startupService.onStartupApplication(this);

        addPreferencesFromResource(R.xml.preferences_oldtasks);

        findPreference(getString(R.string.p_show_deleted_tasks)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                result.putBoolean(TOGGLE_DELETED, true);
                setResult(RESULT_OK, new Intent().putExtras(result));
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(EXTRA_RESULT, result);
    }


}
