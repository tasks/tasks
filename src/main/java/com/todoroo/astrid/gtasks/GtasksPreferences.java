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
import org.tasks.activities.ClearGtaskDataActivity;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDate;

public class GtasksPreferences extends InjectingPreferenceActivity {

    private static final int REQUEST_LOGIN = 0;
    private static final int REQUEST_LOGOUT = 1;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject BackgroundScheduler backgroundScheduler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_gtasks);

        Preference gtaskPreference = findPreference(getString(R.string.sync_gtasks));
        ((CheckBoxPreference) gtaskPreference).setChecked(gtasksPreferenceService.isLoggedIn());
        gtaskPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue && !gtasksPreferenceService.isLoggedIn()) {
                    startActivityForResult(new Intent(GtasksPreferences.this, GtasksLoginActivity.class), REQUEST_LOGIN);
                } else {
                    gtasksPreferenceService.stopOngoing();
                    gtasksPreferenceService.setToken(null);
                }
                return true;
            }
        });
        if (gtasksPreferenceService.getLastSyncDate() > 0) {
            gtaskPreference.setSummary(getString(R.string.sync_status_success,
                    DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                            newDate(gtasksPreferenceService.getLastSyncDate()))));
        }
        findPreference(getString(R.string.sync_SPr_forget_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(GtasksPreferences.this, ClearGtaskDataActivity.class), REQUEST_LOGOUT);
                return true;
            }
        });
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
    protected void onPause() {
        super.onPause();
        backgroundScheduler.scheduleGtaskSync();
    }
}
