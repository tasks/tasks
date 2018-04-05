package org.tasks.preferences;

import static org.tasks.PermissionUtil.verifyPermissions;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import com.todoroo.astrid.voice.VoiceOutputAssistant;
import java.io.File;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.files.FileExplore;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.scheduling.CalendarNotificationIntentService;
import timber.log.Timber;

public class MiscellaneousPreferences extends InjectingPreferenceActivity {

  private static final int REQUEST_CODE_FILES_DIR = 2;
  private static final int REQUEST_CODE_TTS_CHECK = 2534;

  @Inject Preferences preferences;
  @Inject VoiceOutputAssistant voiceOutputAssistant;
  @Inject ActivityPermissionRequestor permissionRequestor;
  @Inject PermissionChecker permissionChecker;

  private CheckBoxPreference calendarReminderPreference;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_misc);

    calendarReminderPreference =
        (CheckBoxPreference) findPreference(getString(R.string.p_calendar_reminders));

    initializeAttachmentDirectoryPreference();
    initializeCalendarReminderPreference();
    initializeVoiceReminderPreference();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_FILES_DIR && resultCode == RESULT_OK) {
      if (data != null) {
        String dir = data.getStringExtra(FileExplore.EXTRA_DIRECTORY);
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
      Timber.e(e);
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    voiceOutputAssistant.shutdown();
  }

  private void initializeAttachmentDirectoryPreference() {
    findPreference(getString(R.string.p_attachment_dir))
        .setOnPreferenceClickListener(
            p -> {
              Intent filesDir = new Intent(MiscellaneousPreferences.this, FileExplore.class);
              filesDir.putExtra(FileExplore.EXTRA_DIRECTORY_MODE, true);
              startActivityForResult(filesDir, REQUEST_CODE_FILES_DIR);
              return true;
            });
    updateAttachmentDirectory();
  }

  private void updateAttachmentDirectory() {
    findPreference(getString(R.string.p_attachment_dir)).setSummary(getAttachmentDirectory());
  }

  private String getAttachmentDirectory() {
    File dir = preferences.getAttachmentsDirectory();
    return dir == null ? "" : dir.getAbsolutePath();
  }

  private void initializeCalendarReminderPreference() {
    CheckBoxPreference calendarReminderPreference =
        (CheckBoxPreference) findPreference(getString(R.string.p_calendar_reminders));
    calendarReminderPreference.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          if (newValue == null) {
            return false;
          }
          if (!(Boolean) newValue) {
            return true;
          }
          if (permissionRequestor.requestCalendarPermissions()) {
            CalendarNotificationIntentService.enqueueWork(this);
            return true;
          }
          return false;
        });
    calendarReminderPreference.setChecked(
        calendarReminderPreference.isChecked() && permissionChecker.canAccessCalendars());
  }

  private void initializeVoiceReminderPreference() {
    findPreference(getString(R.string.p_voiceRemindersEnabled))
        .setOnPreferenceChangeListener(
            (preference, newValue) -> {
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
                Timber.e(e);
                preference.setEnabled(false);
                preferences.setBoolean(preference.getKey(), false);
              }
              return true;
            });
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
      if (verifyPermissions(grantResults)) {
        calendarReminderPreference.setChecked(true);
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
