/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.sync;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.support.annotation.NonNull;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.caldav.CaldavAccountManager;
import org.tasks.data.GoogleTaskDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static org.tasks.PermissionUtil.verifyPermissions;

public class SynchronizationPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_LOGIN = 0;

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
    @Inject CaldavAccountManager caldavAccountManager;
    @Inject Preferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_synchronization);

        CheckBoxPreference caldavEnabled = (CheckBoxPreference) findPreference(getString(R.string.p_sync_caldav));
        caldavEnabled.setChecked(syncAdapters.isCaldavSyncEnabled());
        caldavEnabled.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = ((boolean) newValue) && permissionRequestor.requestCaldavPermissions();
            caldavAccountManager.setBackgroundSynchronization(enabled && preferences.getBoolean(R.string.p_background_sync, true));
            return enabled;
        });
        final CheckBoxPreference gtaskPreference = (CheckBoxPreference) findPreference(getString(R.string.sync_gtasks));
        gtaskPreference.setChecked(syncAdapters.isGoogleTaskSyncEnabled());
        gtaskPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                if (!playServices.refreshAndCheck()) {
                    playServices.resolve(SynchronizationPreferences.this);
                } else if (permissionRequestor.requestAccountPermissions()) {
                    requestLogin();
                }
                return false;
            } else {
                googleAccountManager.setBackgroundSynchronization(false);
                tracker.reportEvent(Tracking.Events.GTASK_DISABLED);
                gtasksPreferenceService.stopOngoing();
                return true;
            }
        });
        if (gtasksPreferenceService.getLastSyncDate() > 0) {
            gtaskPreference.setSummary(getString(R.string.sync_status_success,
                    DateUtilities.getDateStringWithTime(SynchronizationPreferences.this,
                            gtasksPreferenceService.getLastSyncDate())));
        }
        findPreference(getString(R.string.p_background_sync)).setOnPreferenceChangeListener((preference, o) -> {
            boolean backgroundSyncEnabled = (Boolean) o;
            caldavAccountManager.setBackgroundSynchronization(backgroundSyncEnabled);
            googleAccountManager.setBackgroundSynchronization(backgroundSyncEnabled);
            return true;
        });
        findPreference(getString(R.string.sync_SPr_forget_key)).setOnPreferenceClickListener(preference -> {
            dialogBuilder.newMessageDialog(R.string.sync_forget_confirm)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        gtasksPreferenceService.clearLastSyncDate();
                        gtasksPreferenceService.setUserName(null);
                        googleTaskDao.deleteAll();
                        googleAccountManager.setBackgroundSynchronization(false);
                        tracker.reportEvent(Tracking.Events.GTASK_LOGOUT);
                        gtaskPreference.setChecked(false);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
    }

    private void requestLogin() {
        startActivityForResult(new Intent(SynchronizationPreferences.this, GtasksLoginActivity.class), REQUEST_LOGIN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        CheckBoxPreference backgroundSync = (CheckBoxPreference) findPreference(getString(R.string.p_background_sync));
        if (syncAdapters.isMasterSyncEnabled()) {
            backgroundSync.setSummary(null);
        } else {
            backgroundSync.setSummary(R.string.master_sync_warning);
        }
        if (!permissionChecker.canAccessAccounts()) {
            ((CheckBoxPreference) findPreference(getString(R.string.sync_gtasks))).setChecked(false);
            ((CheckBoxPreference) findPreference(getString(R.string.p_sync_caldav))).setChecked(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN) {
            boolean enabled = resultCode == RESULT_OK;
            if (enabled) {
                boolean backgroundSyncEnabled = preferences.getBoolean(R.string.p_background_sync, true);
                googleAccountManager.setBackgroundSynchronization(backgroundSyncEnabled);
                tracker.reportEvent(Tracking.Events.GTASK_ENABLED);
            }
            ((CheckBoxPreference) findPreference(getString(R.string.sync_gtasks))).setChecked(enabled);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_GOOGLE_ACCOUNTS) {
            if (verifyPermissions(grantResults)) {
                requestLogin();
            }
        } else if (requestCode == PermissionRequestor.REQUEST_CALDAV_ACCOUNTS) {
            ((CheckBoxPreference) findPreference(getString(R.string.p_sync_caldav)))
                    .setChecked(verifyPermissions(grantResults));
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
