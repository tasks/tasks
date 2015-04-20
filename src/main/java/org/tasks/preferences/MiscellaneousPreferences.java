package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.speech.tts.TextToSpeech;

import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.gcal.CalendarAlarmScheduler;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

import java.io.File;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.preFroyo;

public class MiscellaneousPreferences extends InjectingPreferenceActivity {

    private static final Logger log = LoggerFactory.getLogger(MiscellaneousPreferences.class);
    private static final int REQUEST_CODE_FILES_DIR = 2;
    private static final int REQUEST_CODE_TTS_CHECK = 2534;

    @Inject Preferences preferences;
    @Inject CalendarAlarmScheduler calendarAlarmScheduler;
    @Inject VoiceOutputAssistant voiceOutputAssistant;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_misc);

        findPreference(getString(R.string.p_debug_logging)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preferences.setupLogger((boolean) newValue);
                return true;
            }
        });

        initializeAttachmentDirectoryPreference();
        initializeCalendarReminderPreference();
        initializeVoiceReminderPreference();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FILES_DIR && resultCode == RESULT_OK) {
            if (data != null) {
                String dir = data.getStringExtra(FileExplore.RESULT_DIR_SELECTED);
                preferences.setString(R.string.p_attachment_dir, dir);
                updateAttachmentDirectory();
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

    private void initializeAttachmentDirectoryPreference() {
        findPreference(getString(R.string.p_attachment_dir)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                Intent filesDir = new Intent(MiscellaneousPreferences.this, FileExplore.class);
                filesDir.putExtra(FileExplore.EXTRA_DIRECTORIES_SELECTABLE, true);
                startActivityForResult(filesDir, REQUEST_CODE_FILES_DIR);
                return true;
            }
        });
        updateAttachmentDirectory();
    }

    private void updateAttachmentDirectory() {
        File dir = preferences.getAttachmentsDirectory();
        String summary = dir == null ? "" : dir.getAbsolutePath();
        findPreference(getString(R.string.p_attachment_dir)).setSummary(summary);
    }

    private void initializeCalendarReminderPreference() {
        Preference calendarReminderPreference = findPreference(getString(R.string.p_calendar_reminders));
        if (preFroyo()) {
            getPreferenceScreen().removePreference(calendarReminderPreference);
        } else {
            calendarReminderPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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

    private void initializeVoiceReminderPreference() {
        findPreference(getString(R.string.p_voiceRemindersEnabled)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (boolean) newValue;
                try {
                    if (enabled && !voiceOutputAssistant.isTTSInitialized()) {
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
                return true;
            }
        });
    }
}
