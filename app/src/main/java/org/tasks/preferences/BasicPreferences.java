package org.tasks.preferences;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.widget.CheckBox;

import com.google.common.base.Strings;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.reminders.ReminderPreferences;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.ColorPickerActivity.ColorPalette;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.analytics.Tracking.Events;
import org.tasks.billing.BillingClient;
import org.tasks.billing.Inventory;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.drive.DriveLoginActivity;
import org.tasks.files.FileHelper;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.locale.Locale;
import org.tasks.locale.LocalePickerDialog;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

import java.io.File;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static org.tasks.dialogs.ExportTasksDialog.newExportTasksDialog;
import static org.tasks.dialogs.ImportTasksDialog.newImportTasksDialog;
import static org.tasks.files.FileHelper.newFilePickerIntent;
import static org.tasks.locale.LocalePickerDialog.newLocalePickerDialog;
import static org.tasks.themes.ThemeColor.LAUNCHERS;

public class BasicPreferences extends InjectingPreferenceActivity
    implements LocalePickerDialog.LocaleSelectionHandler {

  private static final String EXTRA_RESULT = "extra_result";
  private static final String FRAG_TAG_LOCALE_PICKER = "frag_tag_locale_picker";
  private static final String FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks";
  private static final String FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks";
  private static final int RC_PREFS = 10001;
  private static final int REQUEST_THEME_PICKER = 10002;
  private static final int REQUEST_COLOR_PICKER = 10003;
  private static final int REQUEST_ACCENT_PICKER = 10004;
  private static final int REQUEST_CODE_BACKUP_DIR = 10005;
  private static final int REQUEST_PICKER = 10006;
  private static final int REQUEST_LAUNCHER_PICKER = 10007;
  private static final int RC_DRIVE_BACKUP = 10008;
  @Inject Tracker tracker;
  @Inject Preferences preferences;
  @Inject ThemeBase themeBase;
  @Inject ThemeColor themeColor;
  @Inject ThemeAccent themeAccent;
  @Inject DialogBuilder dialogBuilder;
  @Inject Locale locale;
  @Inject ThemeCache themeCache;
  @Inject BillingClient billingClient;
  @Inject Inventory inventory;
  @Inject PlayServices playServices;

  private Bundle result;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_RESULT);

    addPreferencesFromResource(R.xml.preferences);
    if (BuildConfig.DEBUG) {
      addPreferencesFromResource(R.xml.preferences_debug);
    }

    setupActivity(R.string.EPr_appearance_header, AppearancePreferences.class);
    setupActivity(R.string.notifications, ReminderPreferences.class);
    setupActivity(R.string.EPr_manage_header, OldTaskPreferences.class);

    Preference themePreference = findPreference(getString(R.string.p_theme));
    themePreference.setSummary(themeBase.getName());
    themePreference.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(BasicPreferences.this, ColorPickerActivity.class);
          intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPalette.THEMES);
          startActivityForResult(intent, REQUEST_THEME_PICKER);
          return false;
        });
    Preference colorPreference = findPreference(getString(R.string.p_theme_color));
    colorPreference.setSummary(themeColor.getName());
    colorPreference.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(BasicPreferences.this, ColorPickerActivity.class);
          intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPalette.COLORS);
          startActivityForResult(intent, REQUEST_COLOR_PICKER);
          return false;
        });
    Preference accentPreference = findPreference(getString(R.string.p_theme_accent));
    accentPreference.setSummary(themeAccent.getName());
    accentPreference.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(BasicPreferences.this, ColorPickerActivity.class);
          intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPalette.ACCENTS);
          startActivityForResult(intent, REQUEST_ACCENT_PICKER);
          return false;
        });
    Preference launcherPreference = findPreference(getString(R.string.p_theme_launcher));
    ThemeColor launcherColor =
        themeCache.getThemeColor(preferences.getInt(R.string.p_theme_launcher, 7));
    launcherPreference.setSummary(launcherColor.getName());
    launcherPreference.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(BasicPreferences.this, ColorPickerActivity.class);
          intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPalette.LAUNCHER);
          startActivityForResult(intent, REQUEST_LAUNCHER_PICKER);
          return false;
        });
    Preference languagePreference = findPreference(getString(R.string.p_language));
    updateLocale();
    languagePreference.setOnPreferenceClickListener(
        preference -> {
          newLocalePickerDialog().show(getFragmentManager(), FRAG_TAG_LOCALE_PICKER);
          return false;
        });
    findPreference(getString(R.string.p_layout_direction))
        .setOnPreferenceChangeListener(
            (preference, o) -> {
              tracker.reportEvent(
                  Tracking.Events.SET_PREFERENCE, R.string.p_layout_direction, o.toString());
              int newValue = Integer.parseInt((String) o);
              if (locale.getDirectionality()
                  != locale.withDirectionality(newValue).getDirectionality()) {
                showRestartDialog();
              }
              return true;
            });
    findPreference(getString(R.string.p_collect_statistics))
        .setOnPreferenceChangeListener(
            (preference, newValue) -> {
              showRestartDialog();
              return true;
            });

    findPreference(R.string.backup_BAc_import)
        .setOnPreferenceClickListener(
            preference -> {
              startActivityForResult(newFilePickerIntent(BasicPreferences.this, preferences.getBackupDirectory()), REQUEST_PICKER);
              return false;
            });

    findPreference(R.string.backup_BAc_export)
        .setOnPreferenceClickListener(
            preference -> {
              newExportTasksDialog().show(getFragmentManager(), FRAG_TAG_EXPORT_TASKS);
              return false;
            });

    initializeBackupDirectory();

    CheckBoxPreference googleDriveBackup = (CheckBoxPreference) findPreference(R.string.p_google_drive_backup);
    googleDriveBackup.setChecked(preferences.getBoolean(R.string.p_google_drive_backup, false));
    googleDriveBackup
        .setOnPreferenceChangeListener(
            (preference, newValue) -> {
              if (newValue != null && (Boolean) newValue) {
                if (!playServices.refreshAndCheck()) {
                  playServices.resolve(this);
                } else {
                  requestLogin();
                }
              }
              return false;
            });

    requires(
        R.string.settings_localization,
        atLeastJellybeanMR1(),
        R.string.p_language,
        R.string.p_layout_direction);

    //noinspection ConstantConditions
    if (!BuildConfig.FLAVOR.equals("googleplay")) {
      remove(R.string.p_google_drive_backup);
      requires(R.string.privacy, false, R.string.p_collect_statistics);
    }
  }

  private void requestLogin() {
    startActivityForResult(new Intent(this, DriveLoginActivity.class), RC_DRIVE_BACKUP);
  }

  private void setupActivity(int key, final Class<?> target) {
    findPreference(getString(key))
        .setOnPreferenceClickListener(
            preference -> {
              startActivityForResult(new Intent(BasicPreferences.this, target), RC_PREFS);
              return true;
            });
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBundle(EXTRA_RESULT, result);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_THEME_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        preferences.setInt(R.string.p_theme, index);
        themeCache.getThemeBase(index).setDefaultNightMode();
        tracker.reportEvent(Tracking.Events.SET_THEME, Integer.toString(index));
        result.putBoolean(AppearancePreferences.EXTRA_RESTART, true);
        recreate();
      }
    } else if (requestCode == REQUEST_COLOR_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        preferences.setInt(R.string.p_theme_color, index);
        tracker.reportEvent(Tracking.Events.SET_COLOR, Integer.toString(index));
        result.putBoolean(AppearancePreferences.EXTRA_RESTART, true);
        recreate();
      }
    } else if (requestCode == REQUEST_ACCENT_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        preferences.setInt(R.string.p_theme_accent, index);
        tracker.reportEvent(Tracking.Events.SET_ACCENT, Integer.toString(index));
        result.putBoolean(AppearancePreferences.EXTRA_RESTART, true);
        recreate();
      }
    } else if (requestCode == REQUEST_LAUNCHER_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        setLauncherIcon(index);
        preferences.setInt(R.string.p_theme_launcher, index);
        tracker.reportEvent(Events.SET_LAUNCHER, Integer.toString(index));
        recreate();
      }
    } else if (requestCode == RC_PREFS) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        result.putAll(data.getExtras());
      }
    } else if (requestCode == REQUEST_CODE_BACKUP_DIR) {
      if (resultCode == RESULT_OK && data != null) {
        Uri uri = data.getData();
        if (atLeastLollipop()) {
          getContentResolver()
              .takePersistableUriPermission(
                  uri,
                  Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        preferences.setUri(R.string.p_backup_dir, uri);
        updateBackupDirectory();
      }
    } else if (requestCode == REQUEST_PICKER) {
      if (resultCode == RESULT_OK) {
        newImportTasksDialog(data.getData())
            .show(getFragmentManager(), FRAG_TAG_IMPORT_TASKS);
      }
    } else if (requestCode == RC_DRIVE_BACKUP) {
      ((CheckBoxPreference) findPreference(R.string.p_google_drive_backup))
          .setChecked(resultCode == RESULT_OK);
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onLocaleSelected(Locale newValue) {
    String override = newValue.getLanguageOverride();
    if (Strings.isNullOrEmpty(override)) {
      preferences.remove(R.string.p_language);
    } else {
      preferences.setString(R.string.p_language, override);
      tracker.reportEvent(Tracking.Events.SET_PREFERENCE, R.string.p_language, override);
    }
    updateLocale();
    if (!locale.equals(newValue)) {
      showRestartDialog();
    }
  }

  private void updateLocale() {
    Preference languagePreference = findPreference(getString(R.string.p_language));
    String preference = preferences.getStringValue(R.string.p_language);
    languagePreference.setSummary(locale.withLanguage(preference).getDisplayName());
  }

  @Override
  public void finish() {
    Intent data = new Intent();
    data.putExtras(result);
    setResult(Activity.RESULT_OK, data);

    super.finish();
  }

  private void initializeBackupDirectory() {
    findPreference(getString(R.string.p_backup_dir))
        .setOnPreferenceClickListener(
            p -> {
              FileHelper.newDirectoryPicker(this, REQUEST_CODE_BACKUP_DIR, preferences.getBackupDirectory());
              return false;
            });
    updateBackupDirectory();
  }

  private void updateBackupDirectory() {
    findPreference(getString(R.string.p_backup_dir)).setSummary(getBackupDirectory());
  }

  private String getBackupDirectory() {
    Uri uri = preferences.getBackupDirectory();
    return uri.getScheme().equals("file")
        ? new File(uri.getPath()).getAbsolutePath()
        : uri.toString();
  }

  private void setLauncherIcon(int index) {
    PackageManager packageManager = getPackageManager();
    for (int i = 0; i < LAUNCHERS.length; i++) {
      ComponentName componentName =
          new ComponentName(this, "com.todoroo.astrid.activity.TaskListActivity" + LAUNCHERS[i]);
      packageManager.setComponentEnabledSetting(
          componentName,
          index == i
              ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
              : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
          PackageManager.DONT_KILL_APP);
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
