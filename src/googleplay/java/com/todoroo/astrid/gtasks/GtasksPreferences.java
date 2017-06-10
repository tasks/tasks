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
import android.support.annotation.NonNull;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;

import org.tasks.R;
import org.tasks.activities.NativeGoogleTaskListPicker;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleTaskListSelectionHandler;
import org.tasks.gtasks.PlayServicesAvailability;
import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;

import javax.inject.Inject;

import static org.tasks.PermissionUtil.verifyPermissions;

public class GtasksPreferences extends InjectingPreferenceActivity implements GoogleTaskListSelectionHandler {

    private static final String FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection";

    private static final int REQUEST_LOGIN = 0;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject ActivityPermissionRequestor permissionRequestor;
    @Inject GtasksListService gtasksListService;
    @Inject Tracker tracker;
    @Inject SyncAdapterHelper syncAdapterHelper;
    @Inject PlayServicesAvailability playServicesAvailability;
    @Inject DialogBuilder dialogBuilder;
    @Inject MetadataDao metadataDao;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_gtasks);

        final CheckBoxPreference gtaskPreference = (CheckBoxPreference) findPreference(getString(R.string.sync_gtasks));
        gtaskPreference.setChecked(syncAdapterHelper.isEnabled());
        gtaskPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                if (!playServicesAvailability.refreshAndCheck()) {
                    playServicesAvailability.resolve(GtasksPreferences.this);
                } else if (permissionRequestor.requestAccountPermissions()) {
                    requestLogin();
                }
                return false;
            } else {
                syncAdapterHelper.enableSynchronization(false);
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
        findPreference(getString(R.string.gtask_background_sync)).setOnPreferenceChangeListener((preference, o) -> {
            syncAdapterHelper.enableSynchronization((Boolean) o);
            return true;
        });
        findPreference(getString(R.string.sync_SPr_forget_key)).setOnPreferenceClickListener(preference -> {
            dialogBuilder.newMessageDialog(R.string.sync_forget_confirm)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        gtasksPreferenceService.clearLastSyncDate();
                        gtasksPreferenceService.setUserName(null);
                        metadataDao.deleteWhere(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY));
                        syncAdapterHelper.enableSynchronization(false);
                        tracker.reportEvent(Tracking.Events.GTASK_LOGOUT);
                        gtaskPreference.setChecked(false);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
        findPreference(R.string.p_gtasks_default_list).setOnPreferenceClickListener(preference -> {
            new NativeGoogleTaskListPicker()
                    .show(getFragmentManager(), FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
            return false;
        });
        updateDefaultGoogleTaskList();
    }

    private void requestLogin() {
        startActivityForResult(new Intent(GtasksPreferences.this, GtasksLoginActivity.class), REQUEST_LOGIN);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        CheckBoxPreference backgroundSync = (CheckBoxPreference) findPreference(getString(R.string.gtask_background_sync));
        backgroundSync.setChecked(syncAdapterHelper.isSyncEnabled());
        if (syncAdapterHelper.isMasterSyncEnabled()) {
            backgroundSync.setSummary(null);
        } else {
            backgroundSync.setSummary(R.string.master_sync_warning);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN) {
            boolean enabled = resultCode == RESULT_OK;
            if (enabled) {
                syncAdapterHelper.enableSynchronization(true);
                tracker.reportEvent(Tracking.Events.GTASK_ENABLED);
            }
            ((CheckBoxPreference) findPreference(getString(R.string.sync_gtasks))).setChecked(enabled);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_ACCOUNTS) {
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

    @Override
    public void selectedList(GtasksList list) {
        tracker.reportEvent(Tracking.Events.GTASK_DEFAULT_LIST);
        String listId = list.getRemoteId();
        gtasksPreferenceService.setDefaultList(listId);
        updateDefaultGoogleTaskList();
    }

    private void updateDefaultGoogleTaskList() {
        GtasksList list = gtasksListService.getList(gtasksPreferenceService.getDefaultList());
        if (list != null) {
            findPreference(R.string.p_gtasks_default_list).setSummary(list.getName());
        }
    }
}
