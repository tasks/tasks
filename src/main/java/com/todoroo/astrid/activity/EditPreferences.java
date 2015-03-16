/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.backup.BackupPreferences;
import com.todoroo.astrid.core.DefaultsPreferences;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.gcal.CalendarAlarmScheduler;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.reminders.ReminderPreferences;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.preFroyo;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class EditPreferences extends TodorooPreferenceActivity {

    private static final Logger log = LoggerFactory.getLogger(EditPreferences.class);
    private static final int REQUEST_CODE_FILES_DIR = 2;
    private static final int REQUEST_CODE_TTS_CHECK = 2534;

    public static final int RESULT_CODE_PERFORMANCE_PREF_CHANGED = 3;

    // --- instance variables

    @Inject StartupService startupService;
    @Inject Preferences preferences;
    @Inject CalendarAlarmScheduler calendarAlarmScheduler;
    @Inject VoiceOutputAssistant voiceOutputAssistant;

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

        addPreferencesFromResource(R.xml.preferences_misc);

        final Resources r = getResources();

        // first-order preferences

        Preference beastMode = findPreference(getString(R.string.p_beastMode));
        beastMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showBeastMode();
                return true;
            }
        });

        Preference preference = screen.findPreference(getString(R.string.p_files_dir));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                Intent filesDir = new Intent(EditPreferences.this, FileExplore.class);
                filesDir.putExtra(FileExplore.EXTRA_DIRECTORIES_SELECTABLE, true);
                startActivityForResult(filesDir, REQUEST_CODE_FILES_DIR);
                return true;
            }
        });

        addPreferenceListeners();

        removeForbiddenPreferences(screen, r);
    }

    public static void removeForbiddenPreferences(PreferenceScreen screen, Resources r) {
        int[] forbiddenPrefs = Constants.MARKET_STRATEGY.excludedSettings();
        if (forbiddenPrefs == null) {
            return;
        }
        for (int i : forbiddenPrefs) {
            searchForAndRemovePreference(screen, r.getString(i));
        }
    }

    private static boolean searchForAndRemovePreference(PreferenceGroup group, String key) {
        int preferenceCount = group.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final Preference preference = group.getPreference(i);
            final String curKey = preference.getKey();

            if (curKey != null && curKey.equals(key)) {
                group.removePreference(preference);
                return true;
            }

            if (preference instanceof PreferenceGroup) {
                if (searchForAndRemovePreference((PreferenceGroup) preference, key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void showBeastMode() {
        Intent intent = new Intent(this, BeastModePreferences.class);
        intent.setAction(AstridApiConstants.ACTION_SETTINGS);
        startActivity(intent);
    }

    private void addPreferences(PreferenceScreen screen) {
        List<Preference> preferences = new ArrayList<Preference>() {{
            add(getPreference(ReminderPreferences.class, R.string.notifications));
            add(getPreference(DefaultsPreferences.class, R.string.task_defaults));
            add(getPreference(GtasksPreferences.class, R.string.gtasks_GPr_header));
            add(getPreference(BackupPreferences.class, R.string.backup_BPr_header));
            add(getPreference(OldTaskPreferences.class, R.string.EPr_manage_header));
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void updatePreferences(final Preference preference, Object value) {
        final Resources r = getResources();

        if (r.getString(R.string.p_files_dir).equals(preference.getKey())) {
            String dir = preferences.getStringValue(TaskAttachment.FILES_DIRECTORY_PREF);
            preference.setSummary(TextUtils.isEmpty(dir)
                    ? r.getString(R.string.p_files_dir_desc_default)
                    : dir);
        } else if (r.getString(R.string.p_voiceRemindersEnabled).equals(preference.getKey())) {
            onVoiceReminderStatusChanged(preference, (Boolean) value);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        voiceOutputAssistant.shutdown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FILES_DIR && resultCode == RESULT_OK) {
            if (data != null) {
                String dir = data.getStringExtra(FileExplore.RESULT_DIR_SELECTED);
                preferences.setString(TaskAttachment.FILES_DIRECTORY_PREF, dir);
            }
            return;
        }
        try {
            if (requestCode == REQUEST_CODE_TTS_CHECK) {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // success, create the TTS instance
                    voiceOutputAssistant.initTTS();
                } else {
                    // missing data, install it
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                }
            }
        } catch (VerifyError e) {
            // unavailable
            log.error(e.getMessage(), e);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addPreferenceListeners() {
        findPreference(getString(R.string.p_use_dark_theme)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_fontSize)).setOnPreferenceChangeListener(new SetResultOnPreferenceChangeListener(RESULT_CODE_PERFORMANCE_PREF_CHANGED));

        findPreference(getString(R.string.p_debug_logging)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preferences.setupLogger((boolean) newValue);
                return true;
            }
        });

        if (preFroyo()) {
            searchForAndRemovePreference(getPreferenceScreen(), getString(R.string.p_calendar_reminders));
        } else {
            findPreference(getString(R.string.p_calendar_reminders)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null && ((Boolean) newValue)) {
                        calendarAlarmScheduler.scheduleCalendarAlarms(EditPreferences.this, true);
                    }
                    return true;
                }
            });
        }

        findPreference(getString(R.string.p_fullTaskTitle)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updatePreferences(preference, newValue);
                return true;
            }
        });
    }

    private void onVoiceReminderStatusChanged(final Preference preference, boolean enabled) {
        try {
            if(enabled && !voiceOutputAssistant.isTTSInitialized()) {
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, REQUEST_CODE_TTS_CHECK);
            } else if (!enabled && voiceOutputAssistant.isTTSInitialized()) {
                voiceOutputAssistant.shutdown();
            }
        } catch (VerifyError e) {
            log.error(e.getMessage(), e);
            preference.setEnabled(false);
            preferences.setBoolean(preference.getKey(), false);
        }
    }
}
