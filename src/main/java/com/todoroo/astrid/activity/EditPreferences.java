/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.backup.BackupPreferences;
import com.todoroo.astrid.core.DefaultsPreferences;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.reminders.ReminderPreferences;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.AppearancePreferences;
import org.tasks.preferences.MiscellaneousPreferences;

import java.util.ArrayList;
import java.util.List;

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

        List<Preference> preferences = new ArrayList<Preference>() {{
            add(getPreference(AppearancePreferences.class, R.string.EPr_appearance_header));
            add(getPreference(ReminderPreferences.class, R.string.notifications));
            add(getPreference(DefaultsPreferences.class, R.string.task_defaults));
            add(getPreference(GtasksPreferences.class, R.string.gtasks_GPr_header));
            add(getPreference(BackupPreferences.class, R.string.backup_BPr_header));
            add(getPreference(OldTaskPreferences.class, R.string.EPr_manage_header));
            add(getPreference(MiscellaneousPreferences.class, R.string.miscellaneous));
        }};

        PreferenceScreen screen= getPreferenceScreen();
        for (Preference preference : preferences) {
            screen.addPreference(preference);
        }
    }

    private Preference getPreference(final Class<? extends TodorooPreferenceActivity> klass, final int label) {
        return new Preference(this) {{
            setTitle(getResources().getString(label));
            setIntent(new Intent(EditPreferences.this, klass) {{
                setAction(AstridApiConstants.ACTION_SETTINGS);
            }});
        }};
    }
}
