package com.todoroo.astrid.sync;

import java.util.Date;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;

import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.api.R;

/**
 * Utility class for common synchronization action: displaying synchronization
 * preferences and an action panel so users can initiate actions from the menu.
 *
 * @author Tim Su <tim@todoroo.com
 *
 */
abstract public class SyncProviderPreferences extends TodorooPreferenceActivity {

    // --- interface

    public static final int RESULT_CODE_SYNCHRONIZE = 2;

    /**
     * @return your preference resource
     */
    @Override
    abstract public int getPreferenceResource();

    /**
     * kick off synchronization
     */
    abstract public void startSync();

    /**
     * log user out
     */
    abstract public void logOut();

    /**
     * @return get preference utilities
     */
    abstract public SyncProviderUtilities getUtilities();


    protected static final int REQUEST_LOGIN = 0;

    // --- implementation

    private int statusColor = Color.BLACK;

    public SyncProviderPreferences() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setOnHierarchyChangeListener(new OnHierarchyChangeListener() {

            @Override
            public void onChildViewRemoved(View parent, View child) {
                //
            }

            @Override
            public void onChildViewAdded(View parent, View child) {
                View view = findViewById(R.id.status);
                if(view != null)
                    view.setBackgroundColor(statusColor);
            }
        });
    }

    /**
     *
     * @param resource
     *            if null, updates all resources
     */
    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        // interval
        if (r.getString(getUtilities().getSyncIntervalKey()).equals(
                preference.getKey())) {
            int index = AndroidUtilities.indexOf(
                    r.getStringArray(R.array.sync_SPr_interval_values),
                    (String) value);
            if (index <= 0)
                preference.setSummary(R.string.sync_SPr_interval_desc_disabled);
            else
                preference.setSummary(r.getString(
                        R.string.sync_SPr_interval_desc,
                        r.getStringArray(R.array.sync_SPr_interval_entries)[index]));
        }

        // status
        else if (r.getString(R.string.sync_SPr_status_key).equals(preference.getKey())) {
            boolean loggedIn = getUtilities().isLoggedIn();
            String status;
            //String subtitle = ""; //$NON-NLS-1$

            // ! logged in - display message, click -> sync
            if(!loggedIn) {
                status = r.getString(R.string.sync_status_loggedout);
                statusColor = Color.rgb(19, 132, 165);
            }
            // sync is occurring
            else if(getUtilities().isOngoing()) {
                status = r.getString(R.string.sync_status_ongoing);
                statusColor = Color.rgb(0, 0, 100);
            }
            // last sync had errors
            else if(getUtilities().getLastError() != null || getUtilities().getLastAttemptedSyncDate() != 0) {
                // last sync was failure
                if(getUtilities().getLastAttemptedSyncDate() != 0) {
                    status = r.getString(R.string.sync_status_failed,
                        DateUtilities.getDateStringWithTime(SyncProviderPreferences.this,
                        new Date(getUtilities().getLastAttemptedSyncDate())));
                    statusColor = Color.rgb(100, 0, 0);

                    if(getUtilities().getLastSyncDate() > 0) {
//                        subtitle = r.getString(R.string.sync_status_failed_subtitle,
//                                DateUtilities.getDateStringWithTime(SyncProviderPreferences.this,
//                                        new Date(getUtilities().getLastSyncDate())));
                    }
                } else {
                    long lastSyncDate = getUtilities().getLastSyncDate();
                    String dateString = lastSyncDate > 0 ?
                            DateUtilities.getDateStringWithTime(SyncProviderPreferences.this,
                                    new Date(lastSyncDate)) : ""; //$NON-NLS-1$
                    status = r.getString(R.string.sync_status_errors, dateString);
                    statusColor = Color.rgb(100, 100, 0);
                }
            }
            else if(getUtilities().getLastSyncDate() > 0) {
                status = r.getString(R.string.sync_status_success,
                        DateUtilities.getDateStringWithTime(SyncProviderPreferences.this,
                        new Date(getUtilities().getLastSyncDate())));
                statusColor = Color.rgb(0, 100, 0);
            } else {
                status = r.getString(R.string.sync_status_never);
                statusColor = Color.rgb(0, 0, 100);
            }
            preference.setTitle(R.string.sync_SPr_sync);
            preference.setSummary(r.getString(R.string.sync_SPr_status_subtitle, status));

            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    startSync();
                    return true;
                }
            });

            View view = findViewById(R.id.status);
            if(view != null)
                view.setBackgroundColor(statusColor);
        }
        // log out button
        else if (r.getString(R.string.sync_SPr_forget_key).equals(preference.getKey())) {
            boolean loggedIn = getUtilities().isLoggedIn();
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    DialogUtilities.okCancelDialog(SyncProviderPreferences.this,
                            r.getString(R.string.sync_forget_confirm), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            logOut();
                            initializePreference(getPreferenceScreen());
                        }
                    }, null);
                    return true;
                }
            });
            if(!loggedIn) {
                PreferenceCategory category = (PreferenceCategory) findPreference(r.getString(R.string.sync_SPr_key_options));
                category.removePreference(preference);
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            setResult(RESULT_CODE_SYNCHRONIZE);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}