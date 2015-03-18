/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;

import org.tasks.R;
import org.tasks.scheduling.BackgroundScheduler;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDate;

public class GtasksPreferences extends TodorooPreferenceActivity {

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject BackgroundScheduler backgroundScheduler;

    private void startSync() {
        if (!gtasksPreferenceService.isLoggedIn()) {
            startLogin();
        } else {
            syncOrImport();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            syncOrImport();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void syncOrImport() {
        setResultForSynchronize();
    }

    private void setResultForSynchronize() {
        setResult(RESULT_CODE_SYNCHRONIZE);
        finish();
    }

    private void startLogin() {
        Intent intent = new Intent(this, GtasksLoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        backgroundScheduler.scheduleGtaskSync();
    }

    public static final int RESULT_CODE_SYNCHRONIZE = 2;

    protected static final int REQUEST_LOGIN = 0;

    // --- implementation

    private int statusColor = Color.BLACK;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_gtasks);

        getListView().setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewRemoved(View parent, View child) {
                //
            }

            @Override
            public void onChildViewAdded(View parent, View child) {
                View view = findViewById(R.id.status);
                if(view != null) {
                    view.setBackgroundColor(statusColor);
                }
            }
        });
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        if (getString(R.string.sync_SPr_status_key).equals(preference.getKey())) {
            updateStatus(preference);
        }
    }

    private void updateStatus(Preference preference) {
        boolean loggedIn = gtasksPreferenceService.isLoggedIn();
        String status;
        //String subtitle = ""; //$NON-NLS-1$

        // ! logged in - display message, click -> sync
        if(!loggedIn) {
            status = getString(R.string.sync_status_loggedout);
            statusColor = Color.rgb(19, 132, 165);
        }
        // sync is occurring
        else if(gtasksPreferenceService.isOngoing()) {
            status = getString(R.string.sync_status_ongoing);
            statusColor = Color.rgb(0, 0, 100);
        }
        // last sync had errors
        else if(gtasksPreferenceService.getLastError() != null || gtasksPreferenceService.getLastAttemptedSyncDate() != 0) {
            // last sync was failure
            if(gtasksPreferenceService.getLastAttemptedSyncDate() != 0) {
                status = getString(R.string.sync_status_failed,
                        DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                                newDate(gtasksPreferenceService.getLastAttemptedSyncDate())));
                statusColor = Color.rgb(100, 0, 0);
            } else {
                long lastSyncDate = gtasksPreferenceService.getLastSyncDate();
                String dateString = lastSyncDate > 0 ?
                        DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                                newDate(lastSyncDate)) : ""; //$NON-NLS-1$
                status = getString(R.string.sync_status_errors, dateString);
                statusColor = Color.rgb(100, 100, 0);
            }
        }
        else if(gtasksPreferenceService.getLastSyncDate() > 0) {
            status = getString(R.string.sync_status_success,
                    DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                            newDate(gtasksPreferenceService.getLastSyncDate())));
            statusColor = Color.rgb(0, 100, 0);
        } else {
            status = getString(R.string.sync_status_never);
            statusColor = Color.rgb(0, 0, 100);
        }
        preference.setTitle(R.string.sync_SPr_sync);
        preference.setSummary(getString(R.string.sync_SPr_status_subtitle, status));

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                startSync();
                return true;
            }
        });

        View view = findViewById(R.id.status);
        if(view != null) {
            view.setBackgroundColor(statusColor);
        }
    }
}
