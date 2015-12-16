package org.tasks.preferences;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.speech.tts.TextToSpeech;

import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.voice.VoiceOutputAssistant;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.scheduling.BackgroundScheduler;

import java.io.File;

import javax.inject.Inject;

import timber.log.Timber;

public class MiscellaneousPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_CODE_FILES_DIR = 2;
    private static final int REQUEST_CODE_TTS_CHECK = 2534;

    @Inject Preferences preferences;
    @Inject VoiceOutputAssistant voiceOutputAssistant;
    @Inject PermissionRequestor permissionRequestor;
    @Inject PermissionChecker permissionChecker;
    @Inject BackgroundScheduler backgroundScheduler;

    private CheckBoxPreference calendarReminderPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_misc);

        calendarReminderPreference = (CheckBoxPreference) findPreference(getString(R.string.p_calendar_reminders));

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
            Timber.e(e, e.getMessage());
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
        CheckBoxPreference calendarReminderPreference = (CheckBoxPreference) findPreference(getString(R.string.p_calendar_reminders));
        calendarReminderPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue == null) {
                    return false;
                }
                if (!(Boolean) newValue) {
                    return true;
                }
                if (permissionRequestor.requestCalendarPermissions()) {
                    backgroundScheduler.scheduleCalendarNotifications();
                    return true;
                }
                return false;
            }
        });
        calendarReminderPreference.setChecked(calendarReminderPreference.isChecked() && permissionChecker.canAccessCalendars());
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
                    Timber.e(e, e.getMessage());
                    preference.setEnabled(false);
                    preferences.setBoolean(preference.getKey(), false);
                }
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                calendarReminderPreference.setChecked(true);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
