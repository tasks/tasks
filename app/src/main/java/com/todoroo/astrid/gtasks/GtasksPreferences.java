/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.PlayServices;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.ActivityPermissionRequestor;

import javax.inject.Inject;

public class GtasksPreferences extends InjectingPreferenceActivity  {

    private static final int REQUEST_LOGIN = 0;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject ActivityPermissionRequestor permissionRequestor;
    @Inject GtasksListService gtasksListService;
    @Inject Tracker tracker;
    @Inject GtaskSyncAdapterHelper syncAdapterHelper;
    @Inject PlayServices playServices;
    @Inject DialogBuilder dialogBuilder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_google_tasks);

        final CheckBoxPreference gtaskPreference = (CheckBoxPreference) findPreference(getString(R.string.sync_gtasks));
        gtaskPreference.setChecked(syncAdapterHelper.isEnabled());
        gtaskPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                if (!playServices.refreshAndCheck()) {
                    playServices.resolve(GtasksPreferences.this);
                } else if (permissionRequestor.requestAccountPermissions()) {
                    requestLogin();
                }
                return false;
            } else {
                //TODO syncAdapterHelper.enableSynchronization(false);
                tracker.reportEvent(Tracking.Events.GTASK_DISABLED);
                gtasksPreferenceService.stopOngoing();
                return true;
            }
        });
        if (gtasksPreferenceService.getLastSyncDate() > 0) {
            gtaskPreference.setSummary(getString(R.string.sync_status_success,
                    DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                            gtasksPreferenceService.getLastSyncDate())));
        }
        findPreference(getString(R.string.p_background_sync)).setOnPreferenceChangeListener((preference, o) -> {
            //TODO syncAdapterHelper.enableSynchronization((Boolean) o);
            return true;
        });
        boolean useNote = gtasksPreferenceService.getUseNoteForMetadataSync();
        final CheckBoxPreference gtaskUseNotePreference = (CheckBoxPreference) findPreference(getString(R.string.gtasks_sync_metadata_using_note_key));
        gtaskUseNotePreference.setChecked(useNote);
        gtaskUseNotePreference.setOnPreferenceChangeListener((preference, o) -> {
            String summary = getString(((Boolean) o)?R.string.sync_force_add:R.string.sync_force_delete);
            findPreference(getString(R.string.sync_force)).setSummary(summary);
            return true;
        });
        Preference gtaskForcePreference = findPreference(getString(R.string.sync_force));
        gtaskForcePreference.setEnabled(!gtasksPreferenceService.isOngoing());
        gtaskForcePreference.setOnPreferenceClickListener(preference -> {
            gtaskForcePreference.setEnabled(false);
            if (!gtasksPreferenceService.isOngoing()) {
                syncAdapterHelper.initiateManualFullSync();
            }
            return true;
        });
        findPreference(getString(R.string.sync_force)).setSummary(getString(useNote?R.string.sync_force_add:R.string.sync_force_delete));
        findPreference(getString(R.string.sync_SPr_forget_key)).setOnPreferenceClickListener(preference -> {
            dialogBuilder.newMessageDialog(R.string.sync_forget_confirm)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        gtasksPreferenceService.clearLastSyncDate();
                        gtasksPreferenceService.setUserName(null);
                        // TODO syncAdapterHelper.enableSynchronization(false);
                        tracker.reportEvent(Tracking.Events.GTASK_LOGOUT);
                        gtaskPreference.setChecked(false);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
    }

    private void requestLogin() {
        startActivityForResult(new Intent(GtasksPreferences.this, GtasksLoginActivity.class), REQUEST_LOGIN);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        CheckBoxPreference backgroundSync = (CheckBoxPreference) findPreference(getString(R.string.p_background_sync));
        backgroundSync.setChecked(syncAdapterHelper.isEnabled());
        // TODO if (syncAdapterHelper.isMasterSyncEnabled()) {
            backgroundSync.setSummary(null);
        //} else {
        //    backgroundSync.setSummary(R.string.master_sync_warning);
        //}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN) {
            boolean enabled = resultCode == RESULT_OK;
            if (enabled) {
                // TODO syncAdapterHelper.enableSynchronization(true);
                tracker.reportEvent(Tracking.Events.GTASK_ENABLED);
            }
            ((CheckBoxPreference) findPreference(getString(R.string.sync_gtasks))).setChecked(enabled);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }



    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

}
