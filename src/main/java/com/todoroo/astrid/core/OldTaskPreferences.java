/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.os.Bundle;

import com.todoroo.astrid.service.StartupService;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

public class OldTaskPreferences extends InjectingPreferenceActivity {

    @Inject StartupService startupService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startupService.onStartupApplication(this);

        addPreferencesFromResource(R.xml.preferences_oldtasks);
    }
}
