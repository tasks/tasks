/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.sync;

import static org.tasks.PermissionUtil.verifyPermissions;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.annotation.NonNull;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.caldav.CaldavAccountSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.jobs.JobManager;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;

public class SynchronizationPreferences extends InjectingPreferenceActivity {

  private static final int REQUEST_LOGIN = 0;
  private static final int REQUEST_CALDAV_SETTINGS = 101;

  @Inject GtasksPreferenceService gtasksPreferenceService;
  @Inject ActivityPermissionRequestor permissionRequestor;
  @Inject PermissionChecker permissionChecker;
  @Inject Tracker tracker;
  @Inject GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  @Inject PlayServices playServices;
  @Inject DialogBuilder dialogBuilder;
  @Inject SyncAdapters syncAdapters;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject GoogleAccountManager googleAccountManager;
  @Inject Preferences preferences;
  @Inject JobManager jobManager;
  @Inject CaldavDao caldavDao;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_synchronization);

    PreferenceCategory caldavPreferences = (PreferenceCategory) findPreference(getString(R.string.CalDAV));
    for (CaldavAccount caldavAccount : caldavDao.getAccounts()) {
      Preference accountPreferences = new Preference(this);
      accountPreferences.setTitle(caldavAccount.getName());
      accountPreferences.setOnPreferenceClickListener(preference -> {
        Intent intent = new Intent(this, CaldavAccountSettingsActivity.class);
        intent.putExtra(CaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, caldavAccount);
        startActivityForResult(intent, REQUEST_CALDAV_SETTINGS);
        return false;
      });
      caldavPreferences.addPreference(accountPreferences);
    }
    findPreference(getString(R.string.add_account)).setOnPreferenceClickListener(
        preference -> {
          startActivityForResult(new Intent(this, CaldavAccountSettingsActivity.class),
              REQUEST_CALDAV_SETTINGS);
          return false;
        });

    final CheckBoxPreference gtaskPreference =
        (CheckBoxPreference) findPreference(getString(R.string.sync_gtasks));
    gtaskPreference.setChecked(syncAdapters.isGoogleTaskSyncEnabled());
    gtaskPreference.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          if ((boolean) newValue) {
            if (!playServices.refreshAndCheck()) {
              playServices.resolve(SynchronizationPreferences.this);
            } else if (permissionRequestor.requestAccountPermissions()) {
              requestLogin();
            }
            return false;
          } else {
            jobManager.updateBackgroundSync();
            tracker.reportEvent(Tracking.Events.GTASK_DISABLED);
            return true;
          }
        });
    if (gtasksPreferenceService.getLastSyncDate() > 0) {
      gtaskPreference.setSummary(
          getString(
              R.string.sync_status_success,
              DateUtilities.getDateStringWithTime(
                  SynchronizationPreferences.this, gtasksPreferenceService.getLastSyncDate())));
    }
    findPreference(getString(R.string.p_background_sync_unmetered_only))
        .setOnPreferenceChangeListener(
            (preference, o) -> {
              jobManager.updateBackgroundSync(null, null, (Boolean) o);
              return true;
            });
    findPreference(getString(R.string.p_background_sync))
        .setOnPreferenceChangeListener(
            (preference, o) -> {
              jobManager.updateBackgroundSync(null, (Boolean) o, null);
              return true;
            });
    findPreference(getString(R.string.sync_SPr_forget_key))
        .setOnPreferenceClickListener(
            preference -> {
              dialogBuilder
                  .newMessageDialog(R.string.sync_forget_confirm)
                  .setPositiveButton(
                      android.R.string.ok,
                      (dialog, which) -> {
                        gtasksPreferenceService.clearLastSyncDate();
                        gtasksPreferenceService.setUserName(null);
                        googleTaskDao.deleteAll();
                        tracker.reportEvent(Tracking.Events.GTASK_LOGOUT);
                        gtaskPreference.setChecked(false);
                      })
                  .setNegativeButton(android.R.string.cancel, null)
                  .show();
              return true;
            });
  }

  private void requestLogin() {
    startActivityForResult(
        new Intent(SynchronizationPreferences.this, GtasksLoginActivity.class), REQUEST_LOGIN);
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (!permissionChecker.canAccessAccounts()) {
      ((CheckBoxPreference) findPreference(getString(R.string.sync_gtasks))).setChecked(false);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_LOGIN) {
      boolean enabled = resultCode == RESULT_OK;
      if (enabled) {
        tracker.reportEvent(Tracking.Events.GTASK_ENABLED);
        jobManager.updateBackgroundSync();
      }
      ((CheckBoxPreference) findPreference(getString(R.string.sync_gtasks))).setChecked(enabled);
    } else if (requestCode == REQUEST_CALDAV_SETTINGS) {
      if (resultCode == RESULT_OK) {
        jobManager.updateBackgroundSync();
        Intent intent = getIntent();
        finish();
        startActivity(intent);
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_GOOGLE_ACCOUNTS) {
      if (verifyPermissions(grantResults)) {
        requestLogin();
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
