/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;

import org.tasks.R;
import org.tasks.activities.ClearGtaskDataActivity;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

public class GtasksPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_LOGIN = 0;
    private static final int REQUEST_LOGOUT = 1;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject BackgroundScheduler backgroundScheduler;
    @Inject PermissionRequestor permissionRequestor;

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
                    if (gtasksPreferenceService.isLoggedIn()) {
                        return true;
                    }
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
}
