package com.todoroo.astrid.rmilk;

import java.util.Date;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.widget.TodorooPreferences;
import com.todoroo.astrid.rmilk.sync.RTMSyncProvider;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class MilkPreferences extends TodorooPreferences {

    @Autowired
    private DialogUtilities dialogUtilities;

    private int statusColor = Color.BLACK;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_rmilk;
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

    @Override
    protected void onPause() {
        super.onPause();
        MilkBackgroundService.scheduleService();
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
        if (r.getString(R.string.rmilk_MPr_interval_key).equals(
                preference.getKey())) {
            int index = AndroidUtilities.indexOf(
                    r.getStringArray(R.array.rmilk_MPr_interval_values),
                    (String) value);
            if (index <= 0)
                preference.setSummary(R.string.rmilk_MPr_interval_desc_disabled);
            else
                preference.setSummary(r.getString(
                        R.string.rmilk_MPr_interval_desc,
                        r.getStringArray(R.array.rmilk_MPr_interval_entries)[index]));
        }

        // status
        else if (r.getString(R.string.rmilk_MPr_status_key).equals(preference.getKey())) {
            boolean loggedIn = MilkUtilities.isLoggedIn();
            String status;
            String subtitle = ""; //$NON-NLS-1$

            // ! logged in - display message, click -> sync
            if(!loggedIn) {
                status = r.getString(R.string.rmilk_status_loggedout);
                statusColor = Color.RED;
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        startService(new Intent(MilkPreferences.this, MilkBackgroundService.class));
                        finish();
                        return true;
                    }
                });
            }
            // sync is occurring
            else if(MilkUtilities.isOngoing()) {
                status = r.getString(R.string.rmilk_status_ongoing);
                statusColor = Color.rgb(0, 0, 100);
            }
            // last sync was error
            else if(MilkUtilities.getLastAttemptedSyncDate() != 0) {
                status = r.getString(R.string.rmilk_status_failed,
                        DateUtilities.getDateWithTimeFormat(MilkPreferences.this).
                        format(new Date(MilkUtilities.getLastAttemptedSyncDate())));
                if(MilkUtilities.getLastSyncDate() > 0) {
                    subtitle = r.getString(R.string.rmilk_status_failed_subtitle,
                            DateUtilities.getDateWithTimeFormat(MilkPreferences.this).
                            format(new Date(MilkUtilities.getLastSyncDate())));
                }
                statusColor = Color.rgb(100, 0, 0);
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        String error = MilkUtilities.getLastError();
                        if(error != null)
                            dialogUtilities.okDialog(MilkPreferences.this, error, null);
                        return true;
                    }
                });
            } else if(MilkUtilities.getLastSyncDate() > 0) {
                status = r.getString(R.string.rmilk_status_success,
                        DateUtilities.getDateWithTimeFormat(MilkPreferences.this).
                        format(new Date(MilkUtilities.getLastSyncDate())));
                statusColor = Color.rgb(0, 100, 0);
            } else {
                status = r.getString(R.string.rmilk_status_never);
                statusColor = Color.rgb(0, 0, 100);
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        startService(new Intent(MilkPreferences.this, MilkBackgroundService.class));
                        finish();
                        return true;
                    }
                });
            }
            preference.setTitle(status);
            preference.setSummary(subtitle);

            View view = findViewById(R.id.status);
            if(view != null)
                view.setBackgroundColor(statusColor);
        }

        // sync button
        else if (r.getString(R.string.rmilk_MPr_sync_key).equals(preference.getKey())) {
            boolean loggedIn = MilkUtilities.isLoggedIn();
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    new RTMSyncProvider().synchronize(MilkPreferences.this);
                    finish();
                    return true;
                }
            });
            if(!loggedIn)
                preference.setTitle(R.string.rmilk_MPr_sync_log_in);
        }

        // log out button
        else if (r.getString(R.string.rmilk_MPr_forget_key).equals(preference.getKey())) {
            boolean loggedIn = MilkUtilities.isLoggedIn();
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    dialogUtilities.okCancelDialog(MilkPreferences.this,
                            r.getString(R.string.rmilk_forget_confirm), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            new RTMSyncProvider().signOut();
                            initializePreference(getPreferenceScreen());
                        }
                    }, null);
                    return true;
                }
            });
            if(!loggedIn)
                preference.setEnabled(false);
        }
    }

}