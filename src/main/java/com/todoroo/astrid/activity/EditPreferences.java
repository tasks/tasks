/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.DefaultsPreferences;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.reminders.ReminderPreferences;
import com.todoroo.astrid.service.StartupService;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.MiscellaneousPreferences;

import javax.inject.Inject;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPreferences extends InjectingPreferenceActivity {

    // --- instance variables

    @Inject StartupService startupService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupService.onStartupApplication(this);

        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen screen= getPreferenceScreen();
        addPreferencesFromResource(R.xml.preferences_appearance);
        screen.addPreference(getPreference(ReminderPreferences.class, R.string.notifications));
        screen.addPreference(getPreference(DefaultsPreferences.class, R.string.task_defaults));
        screen.addPreference(getPreference(GtasksPreferences.class, R.string.gtasks_GPr_header));
        addPreferencesFromResource(R.xml.preferences_backup);
        screen.addPreference(getPreference(OldTaskPreferences.class, R.string.EPr_manage_header));
        screen.addPreference(getPreference(MiscellaneousPreferences.class, R.string.miscellaneous));
    }

    private Preference getPreference(final Class<? extends PreferenceActivity> klass, final int label) {
        return new Preference(this) {{
            setTitle(getResources().getString(label));
            setIntent(new Intent(EditPreferences.this, klass) {{
                setAction(AstridApiConstants.ACTION_SETTINGS);
            }});
        }};
    }
}
