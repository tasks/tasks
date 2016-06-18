/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;

import org.tasks.R;
import org.tasks.activities.ClearGtaskDataActivity;
import org.tasks.activities.GoogleTaskListSelectionDialog;
import org.tasks.gtasks.GoogleTaskListSelectionHandler;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

public class GtasksPreferences extends InjectingPreferenceActivity implements GoogleTaskListSelectionHandler {

    private static final String FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection";

    private static final int REQUEST_LOGIN = 0;
    private static final int REQUEST_LOGOUT = 1;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject BackgroundScheduler backgroundScheduler;
    @Inject ActivityPermissionRequestor permissionRequestor;
    @Inject GtasksListService gtasksListService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_gtasks);

        Preference gtaskPreference = findPreference(getString(R.string.sync_gtasks));
        ((CheckBoxPreference) gtaskPreference).setChecked(gtasksPreferenceService.isLoggedIn());
        gtaskPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    if (permissionRequestor.requestAccountPermissions()) {
                        requestLogin();
                    }
                    return false;
                } else {
                    gtasksPreferenceService.stopOngoing();
                    return true;
                }
            }
        });
        if (gtasksPreferenceService.getLastSyncDate() > 0) {
            gtaskPreference.setSummary(getString(R.string.sync_status_success,
                    DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                            gtasksPreferenceService.getLastSyncDate())));
        }
        findPreference(getString(R.string.sync_SPr_forget_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(GtasksPreferences.this, ClearGtaskDataActivity.class), REQUEST_LOGOUT);
                return true;
            }
        });
        getPref(R.string.p_gtasks_default_list).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fragmentManager = getFragmentManager();
                GoogleTaskListSelectionDialog dialog = (GoogleTaskListSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
                if (dialog == null) {
                    dialog = new GoogleTaskListSelectionDialog();
                    dialog.show(fragmentManager, FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
                }
                return false;
            }
        });
        updateDefaultGoogleTaskList();
    }

    private void requestLogin() {
        startActivityForResult(new Intent(GtasksPreferences.this, GtasksLoginActivity.class), REQUEST_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN) {
            ((CheckBoxPreference) findPreference(getString(R.string.sync_gtasks))).setChecked(resultCode == RESULT_OK);
        } else if(requestCode == REQUEST_LOGOUT) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_ACCOUNTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLogin();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        backgroundScheduler.scheduleGtaskSync();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void selectedList(GtasksList list) {
        String listId = list.getRemoteId();
        gtasksPreferenceService.setDefaultList(listId);
        updateDefaultGoogleTaskList();
    }

    private void updateDefaultGoogleTaskList() {
        GtasksList list = gtasksListService.getList(gtasksPreferenceService.getDefaultList());
        if (list != null) {
            getPref(R.string.p_gtasks_default_list).setSummary(list.getName());
        }
    }
}
