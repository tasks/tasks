/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.sync;

import static java.util.Arrays.asList;
import static org.tasks.PermissionUtil.verifyPermissions;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.service.TaskDeleter;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.caldav.CaldavAccountSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.jobs.WorkManager;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;

public class SynchronizationPreferences extends InjectingPreferenceActivity {

  private static final String KEY_ADD_GOOGLE_TASKS = "add_google_tasks";
  private static final String KEY_ADD_CALDAV = "add_caldav";
  private static final int REQUEST_LOGIN = 0;
  private static final int REQUEST_CALDAV_SETTINGS = 101;
  private static final int REQUEST_CALDAV_SUBSCRIBE = 102;
  private static final int REQUEST_GOOGLE_TASKS_SUBSCRIBE = 103;

  @Inject ActivityPermissionRequestor permissionRequestor;
  @Inject PermissionChecker permissionChecker;
  @Inject Tracker tracker;
  @Inject GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
  @Inject PlayServices playServices;
  @Inject DialogBuilder dialogBuilder;
  @Inject SyncAdapters syncAdapters;
  @Inject GoogleTaskDao googleTaskDao;
  @Inject GoogleTaskListDao googleTaskListDao;
  @Inject GoogleAccountManager googleAccountManager;
  @Inject Preferences preferences;
  @Inject WorkManager workManager;
  @Inject CaldavDao caldavDao;
  @Inject Inventory inventory;
  @Inject TaskDeleter taskDeleter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle(R.string.synchronization);
    //noinspection ConstantConditions
    if (BuildConfig.FLAVOR.equals("googleplay")) {
      addPreferencesFromResource(R.xml.preferences_google_tasks);
    }

    addPreferencesFromResource(R.xml.preferences_synchronization);

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
  }

  private void logoutConfirmation(GoogleTaskAccount account) {
    String name = account.getAccount();
    AlertDialog alertDialog =
        dialogBuilder
            .newMessageDialog(R.string.logout_warning, name)
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

  private void requestLogin() {
    startActivityForResult(
        new Intent(SynchronizationPreferences.this, GtasksLoginActivity.class), REQUEST_LOGIN);
  }

  @Override
  protected void onResume() {
    super.onResume();

    //noinspection ConstantConditions
    if (BuildConfig.FLAVOR.equals("googleplay")) {
      addGoogleTasksAccounts();
    }

    addCaldavAccounts();
  }

  private void addGoogleTasksAccounts() {
    PreferenceCategory googleTaskPreferences =
        (PreferenceCategory) findPreference(getString(R.string.gtasks_GPr_header));
    googleTaskPreferences.removeAll();
    for (GoogleTaskAccount googleTaskAccount : googleTaskListDao.getAccounts()) {
      String account = googleTaskAccount.getAccount();
      Preference accountPreferences = new Preference(this);
      accountPreferences.setTitle(account);
      accountPreferences.setSummary(googleTaskAccount.getError());
      accountPreferences.setOnPreferenceClickListener(
          preference -> {
            dialogBuilder
                .newDialog()
                .setTitle(account)
                .setItems(
                    asList(getString(R.string.reinitialize_account), getString(R.string.logout)),
                    (dialog, which) -> {
                      if (which == 0) {
                        addGoogleTaskAccount();
                      } else {
                        logoutConfirmation(googleTaskAccount);
                      }
                    })
                .showThemedListView();
            return false;
          });
      googleTaskPreferences.addPreference(accountPreferences);
    }
    Preference addGoogleTaskAccount = new Preference(this);
    addGoogleTaskAccount.setKey(KEY_ADD_GOOGLE_TASKS);
    addGoogleTaskAccount.setTitle(R.string.add_account);
    if (inventory.hasPro() || googleTaskListDao.getAccounts().isEmpty()) {
      addGoogleTaskAccount.setOnPreferenceClickListener(
          preference -> {
            addGoogleTaskAccount();
            return false;
          });
    } else {
      addGoogleTaskAccount.setSummary(R.string.requires_pro_subscription);
      addGoogleTaskAccount.setOnPreferenceClickListener(
          preference -> {
            startActivityForResult(
                new Intent(this, PurchaseActivity.class), REQUEST_GOOGLE_TASKS_SUBSCRIBE);
            return false;
          });
    }
    googleTaskPreferences.addPreference(addGoogleTaskAccount);
  }

  private void addGoogleTaskAccount() {
    if (!playServices.refreshAndCheck()) {
      playServices.resolve(this);
    } else if (permissionRequestor.requestAccountPermissions()) {
      requestLogin();
    }
  }

  private void addCaldavAccounts() {
    PreferenceCategory caldavPreferences =
        (PreferenceCategory) findPreference(getString(R.string.CalDAV));
    caldavPreferences.removeAll();
    for (CaldavAccount caldavAccount : caldavDao.getAccounts()) {
      Preference accountPreferences = new Preference(this);
      accountPreferences.setTitle(caldavAccount.getName());
      accountPreferences.setSummary(caldavAccount.getError());
      accountPreferences.setOnPreferenceClickListener(
          preference -> {
            Intent intent = new Intent(this, CaldavAccountSettingsActivity.class);
            intent.putExtra(CaldavAccountSettingsActivity.EXTRA_CALDAV_DATA, caldavAccount);
            startActivityForResult(intent, REQUEST_CALDAV_SETTINGS);
            return false;
          });
      caldavPreferences.addPreference(accountPreferences);
    }
    Preference addCaldavAccount = new Preference(this);
    addCaldavAccount.setKey(KEY_ADD_CALDAV);
    addCaldavAccount.setTitle(R.string.add_account);

    if (inventory.hasPro()) {
      addCaldavAccount.setOnPreferenceClickListener(
          preference -> {
            addCaldavAccount();
            return false;
          });
    } else {
      addCaldavAccount.setSummary(R.string.requires_pro_subscription);
      addCaldavAccount.setOnPreferenceClickListener(
          preference -> {
            startActivityForResult(
                new Intent(this, PurchaseActivity.class), REQUEST_CALDAV_SUBSCRIBE);
            return false;
          });
    }
    caldavPreferences.addPreference(addCaldavAccount);
  }

  private void addCaldavAccount() {
    startActivityForResult(
        new Intent(this, CaldavAccountSettingsActivity.class), REQUEST_CALDAV_SETTINGS);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_LOGIN) {
      boolean enabled = resultCode == RESULT_OK;
      if (enabled) {
        tracker.reportEvent(Tracking.Events.GTASK_ENABLED);
        workManager.updateBackgroundSync();
        restart();
      }
    } else if (requestCode == REQUEST_CALDAV_SETTINGS) {
      if (resultCode == RESULT_OK) {
        workManager.updateBackgroundSync();
        restart();
      }
    } else if (requestCode == REQUEST_CALDAV_SUBSCRIBE) {
      if (inventory.hasPro()) {
        addCaldavAccount();
      }
    } else if (requestCode == REQUEST_GOOGLE_TASKS_SUBSCRIBE) {
      if (inventory.hasPro()) {
        requestLogin();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void restart() {
    Intent intent = getIntent();
    finish();
    startActivity(intent);
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
  protected String getHelpUrl() {
    return "http://tasks.org/subscribe";
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
