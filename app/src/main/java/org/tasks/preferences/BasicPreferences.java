package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static java.util.Arrays.asList;
import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.dialogs.ExportTasksDialog.newExportTasksDialog;
import static org.tasks.dialogs.ImportTasksDialog.newImportTasksDialog;
import static org.tasks.dialogs.NativeSeekBarDialog.newSeekBarDialog;
import static org.tasks.files.FileHelper.newFilePickerIntent;
import static org.tasks.files.FileHelper.uri2String;
import static org.tasks.locale.LocalePickerDialog.newLocalePickerDialog;
import static org.tasks.themes.ThemeColor.LAUNCHERS;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.common.base.Strings;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.reminders.ReminderPreferences;
import com.todoroo.astrid.service.TaskDeleter;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.ColorPickerActivity.ColorPalette;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.analytics.Tracking.Events;
import org.tasks.billing.Inventory;
import org.tasks.caldav.CaldavAccountSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.NativeSeekBarDialog;
import org.tasks.drive.DriveLoginActivity;
import org.tasks.etesync.EteSyncAccountSettingsActivity;
import org.tasks.files.FileHelper;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.jobs.WorkManager;
import org.tasks.locale.Locale;
import org.tasks.locale.LocalePickerDialog;
import org.tasks.sync.AddAccountDialog;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.SingleCheckedArrayAdapter;
import org.tasks.ui.Toaster;

public class BasicPreferences extends InjectingPreferenceActivity
    implements LocalePickerDialog.LocaleSelectionHandler, NativeSeekBarDialog.SeekBarCallback {

  private static final String FRAG_TAG_LOCALE_PICKER = "frag_tag_locale_picker";
  private static final String FRAG_TAG_IMPORT_TASKS = "frag_tag_import_tasks";
  private static final String FRAG_TAG_EXPORT_TASKS = "frag_tag_export_tasks";
  private static final String FRAG_TAG_ROW_PADDING_SEEKBAR = "frag_tag_row_padding_seekbar";
  private static final String FRAG_TAG_FONT_SIZE_SEEKBAR = "frag_tag_font_size_seekbar";
  private static final int RC_PREFS = 10001;
  private static final int REQUEST_THEME_PICKER = 10002;
  private static final int REQUEST_COLOR_PICKER = 10003;
  private static final int REQUEST_ACCENT_PICKER = 10004;
  private static final int REQUEST_CODE_BACKUP_DIR = 10005;
  private static final int REQUEST_PICKER = 10006;
  private static final int REQUEST_LAUNCHER_PICKER = 10007;
  private static final int REQUEST_DRIVE_BACKUP = 10008;
  private static final int REQUEST_DEFAULT_LIST = 10009;
  private static final int REQUEST_ROW_PADDING = 10010;
  private static final int REQUEST_FONT_SIZE = 10011;
  private static final int REQUEST_CUSTOMIZE = 10012;
  public static final int REQUEST_CALDAV_SETTINGS = 10013;
  public static final int REQUEST_GOOGLE_TASKS = 10014;

  @Inject Tracker tracker;
  @Inject Preferences preferences;
  @Inject ThemeBase themeBase;
  @Inject ThemeColor themeColor;
  @Inject ThemeAccent themeAccent;
  @Inject DialogBuilder dialogBuilder;
  @Inject Locale locale;
  @Inject ThemeCache themeCache;
  @Inject Inventory inventory;
  @Inject PlayServices playServices;
  @Inject Toaster toaster;
  @Inject ActivityPermissionRequestor permissionRequestor;
  @Inject GoogleAccountManager googleAccountManager;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject WorkManager workManager;
  @Inject GoogleTaskListDao googleTaskListDao;
  @Inject CaldavDao caldavDao;
  @Inject TaskDeleter taskDeleter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);

    setExtraOnChange(
        EXTRA_RESTART,
        R.string.p_fontSize,
        R.string.p_rowPadding,
        R.string.p_fullTaskTitle,
        R.string.p_show_description,
        R.string.p_show_full_description,
        R.string.p_linkify_task_list,
        R.string.p_show_list_indicators);

    findPreference(R.string.p_show_subtasks)
        .setOnPreferenceChangeListener((preference, newValue) -> {
          forceRestart();
          localBroadcastManager.broadcastRefresh();
          return true;
        });

    findPreference(R.string.customize_edit_screen)
        .setOnPreferenceClickListener(
            preference -> {
              startActivityForResult(
                  new Intent(BasicPreferences.this, BeastModePreferences.class),
                  REQUEST_CUSTOMIZE);
              return true;
            });

    findPreference(R.string.p_fontSize)
        .setOnPreferenceClickListener(
            preference -> {
              newSeekBarDialog(
                  R.layout.dialog_font_size_seekbar,
                  10,
                  48,
                  preferences.getFontSize(),
                  REQUEST_FONT_SIZE)
                  .show(getFragmentManager(), FRAG_TAG_FONT_SIZE_SEEKBAR);
              return false;
            });
    updateFontSize();

    findPreference(R.string.p_rowPadding)
        .setOnPreferenceClickListener(
            preference -> {
              newSeekBarDialog(
                  R.layout.dialog_font_size_seekbar,
                  0,
                  16,
                  preferences.getRowPadding(),
                  REQUEST_ROW_PADDING)
                  .show(getFragmentManager(), FRAG_TAG_ROW_PADDING_SEEKBAR);
              return false;
            });
    updateRowPadding();
    Preference defaultList = findPreference(getString(R.string.p_default_list));
    Filter filter = defaultFilterProvider.getDefaultFilter();
    defaultList.setSummary(filter.listingTitle);
    defaultList.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(BasicPreferences.this, FilterSelectionActivity.class);
          intent.putExtra(
              FilterSelectionActivity.EXTRA_FILTER, defaultFilterProvider.getDefaultFilter());
          intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
          startActivityForResult(intent, REQUEST_DEFAULT_LIST);
          return true;
        });

    setupActivity(R.string.notifications, ReminderPreferences.class);
    setupActivity(R.string.EPr_manage_header, OldTaskPreferences.class);
    setupActivity(R.string.debug, DebugPreferences.class);

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

    findPreference(R.string.backup_BAc_import)
        .setOnPreferenceClickListener(
            preference -> {
              startActivityForResult(
                  newFilePickerIntent(BasicPreferences.this, preferences.getBackupDirectory()),
                  REQUEST_PICKER);
              return false;
            });

    findPreference(R.string.backup_BAc_export)
        .setOnPreferenceClickListener(
            preference -> {
              newExportTasksDialog().show(getFragmentManager(), FRAG_TAG_EXPORT_TASKS);
              return false;
            });

    initializeBackupDirectory();

    CheckBoxPreference googleDriveBackup =
        (CheckBoxPreference) findPreference(R.string.p_google_drive_backup);
    googleDriveBackup.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          if (newValue == null) {
            return false;
          }

          if ((Boolean) newValue) {
            if (permissionRequestor.requestAccountPermissions()) {
              requestGoogleDriveLogin();
            }
            return false;
          } else {
            preference.setSummary(null);
            return true;
          }
        });

    findPreference(getString(R.string.p_background_sync_unmetered_only))
        .setOnPreferenceChangeListener(
            (preference, o) -> {
              workManager.updateBackgroundSync(null, null, (Boolean) o);
              return true;
            });
    findPreference(getString(R.string.p_background_sync))
        .setOnPreferenceChangeListener(
            (preference, o) -> {
              workManager.updateBackgroundSync(null, (Boolean) o, null);
              return true;
            });
    CheckBoxPreference positionHack =
        (CheckBoxPreference) findPreference(R.string.google_tasks_position_hack);
    positionHack.setChecked(preferences.isPositionHackEnabled());
    positionHack.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          if (newValue == null) {
            return false;
          }
          preferences.setLong(
              R.string.p_google_tasks_position_hack, ((Boolean) newValue) ? now() : 0);
          return true;
        });
    findPreference(R.string.add_account)
        .setOnPreferenceClickListener(
            preference -> {
              AddAccountDialog.showAddAccountDialog(BasicPreferences.this, dialogBuilder);
              return false;
            });

    requires(
        R.string.settings_localization,
        atLeastJellybeanMR1(),
        R.string.p_language,
        R.string.p_layout_direction);

    requires(R.string.task_list_options, atLeastLollipop(), R.string.p_show_subtasks);

    requires(BuildConfig.DEBUG, R.string.debug);

    //noinspection ConstantConditions
    if (!BuildConfig.FLAVOR.equals("googleplay")) {
      removeGroup(R.string.TEA_control_location);
    }
  }

  private void removeGroup(int key) {
    Preference preference = findPreference(key);
    if (preference != null) {
      ((PreferenceScreen) findPreference(R.string.preference_screen)).removePreference(preference);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    CheckBoxPreference googleDriveBackup =
        (CheckBoxPreference) findPreference(R.string.p_google_drive_backup);
    String account = preferences.getStringValue(R.string.p_google_drive_backup_account);
    if (preferences.getBoolean(R.string.p_google_drive_backup, false)
        && googleAccountManager.canAccessAccount(account)) {
      googleDriveBackup.setChecked(true);
      googleDriveBackup.setSummary(account);
    } else {
      googleDriveBackup.setChecked(false);
    }
    googleDriveBackup.setChecked(preferences.getBoolean(R.string.p_google_drive_backup, false));

    //noinspection ConstantConditions
    if (!BuildConfig.FLAVOR.equals("googleplay")) {
      return;
    }
    List<String> choices =
        asList(getString(R.string.map_provider_mapbox), getString(R.string.map_provider_google));
    SingleCheckedArrayAdapter singleCheckedArrayAdapter =
        new SingleCheckedArrayAdapter(this, choices, themeAccent);
    Preference mapProviderPreference = findPreference(R.string.p_map_provider);
    mapProviderPreference.setOnPreferenceClickListener(
        preference -> {
          dialogBuilder
              .newDialog()
              .setSingleChoiceItems(
                  singleCheckedArrayAdapter,
                  getMapProvider(),
                  (dialog, which) -> {
                    if (which == 1) {
                      if (!playServices.refreshAndCheck()) {
                        playServices.resolve(this);
                        dialog.dismiss();
                        return;
                      }
                    }
                    preferences.setInt(R.string.p_map_provider, which);
                    mapProviderPreference.setSummary(choices.get(which));
                    dialog.dismiss();
                  })
              .setNegativeButton(android.R.string.cancel, null)
              .showThemedListView();
          return false;
        });
    int mapProvider = getMapProvider();
    mapProviderPreference.setSummary(
        mapProvider == -1 ? getString(R.string.none) : choices.get(mapProvider));

    Preference placeProviderPreference = findPreference(R.string.p_place_provider);
    placeProviderPreference.setOnPreferenceClickListener(
        preference -> {
          dialogBuilder
              .newDialog()
              .setSingleChoiceItems(
                  singleCheckedArrayAdapter,
                  getPlaceProvider(),
                  (dialog, which) -> {
                    if (which == 1) {
                      if (!playServices.refreshAndCheck()) {
                        playServices.resolve(this);
                        dialog.dismiss();
                        return;
                      }
                      if (!inventory.hasPro()) {
                        toaster.longToast(R.string.requires_pro_subscription);
                        dialog.dismiss();
                        return;
                      }
                    }
                    preferences.setInt(R.string.p_place_provider, which);
                    placeProviderPreference.setSummary(choices.get(which));
                    dialog.dismiss();
                  })
              .setNegativeButton(android.R.string.cancel, null)
              .showThemedListView();
          return false;
        });
    int placeProvider = getPlaceProvider();
    placeProviderPreference.setSummary(choices.get(placeProvider));

    PreferenceCategory synchronizationPreferences =
        (PreferenceCategory) findPreference(R.string.synchronization);
    synchronizationPreferences.removeAll();

    boolean hasGoogleAccounts = addGoogleTasksAccounts(synchronizationPreferences);
    boolean hasCaldavAccounts = addCaldavAccounts(synchronizationPreferences);
    if (!hasGoogleAccounts) {
      removeGroup(R.string.gtasks_GPr_header);
    }
    if (!(hasGoogleAccounts || hasCaldavAccounts)) {
      removeGroup(R.string.sync_SPr_interval_title);
    }
  }

  private boolean addGoogleTasksAccounts(PreferenceCategory category) {
    List<GoogleTaskAccount> accounts = googleTaskListDao.getAccounts();
    for (GoogleTaskAccount googleTaskAccount : accounts) {
      String account = googleTaskAccount.getAccount();
      Preference preference = new Preference(this);
      preference.setTitle(account);
      String error = googleTaskAccount.getError();
      if (Strings.isNullOrEmpty(error)) {
        preference.setSummary(R.string.gtasks_GPr_header);
      } else {
        preference.setSummary(error);
      }
      preference.setOnPreferenceClickListener(
          p -> {
            dialogBuilder
                .newDialog(account)
                .setItems(
                    asList(getString(R.string.reinitialize_account), getString(R.string.logout)),
                    (dialog, which) -> {
                      if (which == 0) {
                        startActivityForResult(
                            new Intent(this, GtasksLoginActivity.class),
                            BasicPreferences.REQUEST_GOOGLE_TASKS);
                      } else {
                        logoutConfirmation(googleTaskAccount);
                      }
                    })
                .showThemedListView();
            return false;
          });
      category.addPreference(preference);
    }
    return !accounts.isEmpty();
  }

  private boolean addCaldavAccounts(PreferenceCategory category) {
    List<CaldavAccount> accounts = caldavDao.getAccounts();
    for (CaldavAccount account : accounts) {
      Preference preference = new Preference(this);
      preference.setTitle(account.getName());
      String error = account.getError();
      if (Strings.isNullOrEmpty(error)) {
        preference.setSummary(
            account.isCaldavAccount() ? R.string.caldav : R.string.etesync);
      } else {
        preference.setSummary(error);
      }
      preference.setOnPreferenceClickListener(
          p -> {
            Intent intent =
                new Intent(
                    this,
                    account.isCaldavAccount()
                        ? CaldavAccountSettingsActivity.class
                        : EteSyncAccountSettingsActivity.class);
            intent.putExtra(CaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, account);
            startActivityForResult(intent, REQUEST_CALDAV_SETTINGS);
            return false;
          });
      category.addPreference(preference);
    }
    return !accounts.isEmpty();
  }

  private void logoutConfirmation(GoogleTaskAccount account) {
    String name = account.getAccount();
    AlertDialog alertDialog =
        dialogBuilder
            .newDialog()
            .setMessage(R.string.logout_warning, name)
            .setPositiveButton(
                R.string.logout,
                (dialog, which) -> {
                  taskDeleter.delete(account);
                  restart();
                })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    alertDialog.setCanceledOnTouchOutside(false);
    alertDialog.setCancelable(false);
    alertDialog.show();
  }

  private int getPlaceProvider() {
    return playServices.isPlayServicesAvailable() && inventory.hasPro()
        ? preferences.getInt(R.string.p_place_provider, 0)
        : 0;
  }

  private int getMapProvider() {
    return playServices.isPlayServicesAvailable()
        ? preferences.getInt(R.string.p_map_provider, 0)
        : 0;
  }

  private void requestGoogleDriveLogin() {
    startActivityForResult(new Intent(this, DriveLoginActivity.class), REQUEST_DRIVE_BACKUP);
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
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_GOOGLE_ACCOUNTS) {
      if (verifyPermissions(grantResults)) {
        requestGoogleDriveLogin();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_THEME_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        preferences.setInt(R.string.p_theme, index);
        themeCache.getThemeBase(index).setDefaultNightMode();
        tracker.reportEvent(Tracking.Events.SET_THEME, Integer.toString(index));
        forceRestart();
        recreate();
      }
    } else if (requestCode == REQUEST_COLOR_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        preferences.setInt(R.string.p_theme_color, index);
        tracker.reportEvent(Tracking.Events.SET_COLOR, Integer.toString(index));
        forceRestart();
        recreate();
      }
    } else if (requestCode == REQUEST_ACCENT_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        preferences.setInt(R.string.p_theme_accent, index);
        tracker.reportEvent(Tracking.Events.SET_ACCENT, Integer.toString(index));
        forceRestart();
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
        mergeResults(data.getExtras());
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
        Uri uri = data.getData();
        String extension = FileHelper.getExtension(this, uri);
        if (!("json".equalsIgnoreCase(extension) || "xml".equalsIgnoreCase(extension))) {
          toaster.longToast(R.string.invalid_backup_file);
        } else {
          newImportTasksDialog(uri, extension).show(getFragmentManager(), FRAG_TAG_IMPORT_TASKS);
          forceRestart();
        }
      }
    } else if (requestCode == REQUEST_DRIVE_BACKUP) {
      boolean success = resultCode == RESULT_OK;
      ((CheckBoxPreference) findPreference(R.string.p_google_drive_backup)).setChecked(success);
      if (!success && data != null) {
        toaster.longToast(data.getStringExtra(GtasksLoginActivity.EXTRA_ERROR));
      }
    } else if (requestCode == REQUEST_DEFAULT_LIST) {
      if (resultCode == RESULT_OK) {
        Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
        defaultFilterProvider.setDefaultFilter(filter);
        findPreference(getString(R.string.p_default_list)).setSummary(filter.listingTitle);
        localBroadcastManager.broadcastRefresh();
      }
    } else if (requestCode == REQUEST_CUSTOMIZE) {
      if (resultCode == RESULT_OK) {
        forceRestart();
      }
    } else if (requestCode == REQUEST_CALDAV_SETTINGS) {
      if (resultCode == RESULT_OK) {
        workManager.updateBackgroundSync();
        restart();
      }
    } else if (requestCode == REQUEST_GOOGLE_TASKS) {
      if (resultCode == RESULT_OK) {
        workManager.updateBackgroundSync();
        restart();
      } else if (data != null) {
        toaster.longToast(data.getStringExtra(GtasksLoginActivity.EXTRA_ERROR));
      }
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

  private void initializeBackupDirectory() {
    findPreference(getString(R.string.p_backup_dir))
        .setOnPreferenceClickListener(
            p -> {
              FileHelper.newDirectoryPicker(
                  this, REQUEST_CODE_BACKUP_DIR, preferences.getBackupDirectory());
              return false;
            });
    updateBackupDirectory();
  }

  private void updateBackupDirectory() {
    findPreference(getString(R.string.p_backup_dir))
        .setSummary(uri2String(preferences.getBackupDirectory()));
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

  private void updateFontSize() {
    findPreference(R.string.p_fontSize).setSummary(locale.formatNumber(preferences.getFontSize()));
  }

  private void updateRowPadding() {
    findPreference(R.string.p_rowPadding)
        .setSummary(locale.formatNumber(preferences.getRowPadding()));
  }

  @Override
  public void valueSelected(int value, int requestCode) {
    int resId = 0;
    if (requestCode == REQUEST_ROW_PADDING) {
      preferences.setInt(R.string.p_rowPadding, value);
      updateRowPadding();
      resId = R.string.p_rowPadding;
    } else if (requestCode == REQUEST_FONT_SIZE) {
      preferences.setInt(R.string.p_fontSize, value);
      updateFontSize();
      resId = R.string.p_fontSize;
    }
    if (resId > 0) {
      forceRestart();
      tracker.reportEvent(Tracking.Events.SET_PREFERENCE, resId, Integer.toString(value));
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
