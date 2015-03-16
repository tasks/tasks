/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
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
public class EditPreferences extends TodorooPreferenceActivity {

    public static final int RESULT_CODE_PERFORMANCE_PREF_CHANGED = 3;

    // --- instance variables

    @Inject StartupService startupService;

    private class SetResultOnPreferenceChangeListener implements OnPreferenceChangeListener {
        private final int resultCode;
        public SetResultOnPreferenceChangeListener(int resultCode) {
            this.resultCode = resultCode;
        }

        @Override
        public boolean onPreferenceChange(Preference p, Object newValue) {
            setResult(resultCode);
            updatePreferences(p, newValue);
            return true;
        }
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupService.onStartupApplication(this);

        PreferenceScreen screen = getPreferenceScreen();

        addPreferences(screen);

        // first-order preferences

        findPreference(getString(R.string.p_beastMode)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                startActivity(new Intent(EditPreferences.this, BeastModePreferences.class) {{
                    setAction(AstridApiConstants.ACTION_SETTINGS);
                }});
                return true;
            }
        });

        addPreferenceListeners();
    }

    private void addPreferences(PreferenceScreen screen) {
        List<Preference> preferences = new ArrayList<Preference>() {{
            add(getPreference(ReminderPreferences.class, R.string.notifications));
            add(getPreference(DefaultsPreferences.class, R.string.task_defaults));
            add(getPreference(GtasksPreferences.class, R.string.gtasks_GPr_header));
            add(getPreference(BackupPreferences.class, R.string.backup_BPr_header));
            add(getPreference(OldTaskPreferences.class, R.string.EPr_manage_header));
            add(getPreference(MiscellaneousPreferences.class, R.string.miscellaneous));
        }};

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

    @Override
    public void updatePreferences(final Preference preference, Object value) {
    }

    private void addPreferenceListeners() {
        findPreference(getString(R.string.p_use_dark_theme)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_fontSize)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_fullTaskTitle)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updatePreferences(preference, newValue);
                return true;
            }
        });
    }
}
