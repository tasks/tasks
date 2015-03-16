package org.tasks.preferences;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.gcal.CalendarAlarmScheduler;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.preFroyo;

public class MiscellaneousPreferences extends TodorooPreferenceActivity {

    private static final Logger log = LoggerFactory.getLogger(MiscellaneousPreferences.class);
    private static final int REQUEST_CODE_FILES_DIR = 2;
    private static final int REQUEST_CODE_TTS_CHECK = 2534;

    @Inject Preferences preferences;
    @Inject CalendarAlarmScheduler calendarAlarmScheduler;
    @Inject VoiceOutputAssistant voiceOutputAssistant;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen screen = getPreferenceScreen();

        screen.findPreference(getString(R.string.p_files_dir)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                Intent filesDir = new Intent(MiscellaneousPreferences.this, FileExplore.class);
                filesDir.putExtra(FileExplore.EXTRA_DIRECTORIES_SELECTABLE, true);
                startActivityForResult(filesDir, REQUEST_CODE_FILES_DIR);
                return true;
            }
        });

        addPreferenceListeners();
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_misc;
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        voiceOutputAssistant.shutdown();
    }

    private void addPreferenceListeners() {
        findPreference(getString(R.string.p_debug_logging)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preferences.setupLogger((boolean) newValue);
                return true;
            }
        });

        if (preFroyo()) {
            searchForAndRemovePreference(getPreferenceScreen(), getString(R.string.p_calendar_reminders));
        } else {
            findPreference(getString(R.string.p_calendar_reminders)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null && ((Boolean) newValue)) {
                        calendarAlarmScheduler.scheduleCalendarAlarms(MiscellaneousPreferences.this, true);
                    }
                    return true;
                }
            });
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
