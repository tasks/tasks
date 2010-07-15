package com.todoroo.astrid.rmilk;

import java.util.Date;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.ListView;

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
    DialogUtilities dialogUtilities;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_rmilk;
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

        // shortcut
        else if (r.getString(R.string.rmilk_MPr_shortcut_key).equals(
                preference.getKey())) {
            if ((Boolean) value) {
                preference.setSummary(R.string.rmilk_MPr_shortcut_desc_enabled);
            } else {
                preference.setSummary(R.string.rmilk_MPr_shortcut_desc_disabled);
            }
        }

        // status
        else if (r.getString(R.string.rmilk_MPr_status_key).equals(preference.getKey())) {
            boolean loggedIn = Utilities.isLoggedIn();
            String status;
            String subtitle = ""; //$NON-NLS-1$
            int statusColor;

            // ! logged in - display message, click -> sync
            if(!loggedIn) {
                status = r.getString(R.string.rmilk_status_loggedout);
                statusColor = Color.RED;
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        new RTMSyncProvider().synchronize(MilkPreferences.this);
                        return true;
                    }
                });
            }
            // last sync was error
            else if(Utilities.getLastAttemptedSyncDate() != 0) {
                status = r.getString(R.string.rmilk_status_failed,
                        DateUtilities.getDateWithTimeFormat(MilkPreferences.this).
                        format(new Date(Utilities.getLastAttemptedSyncDate())));
                if(Utilities.getLastSyncDate() > 0) {
                    subtitle = r.getString(R.string.rmilk_status_success,
                            DateUtilities.getDateWithTimeFormat(MilkPreferences.this).
                            format(new Date(Utilities.getLastSyncDate())));
                }
                statusColor = Color.rgb(100, 0, 0);
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        String error = Utilities.getLastError();
                        if(error != null)
                            dialogUtilities.okDialog(MilkPreferences.this, error, null);
                        return true;
                    }
                });
            } else {
                status = r.getString(R.string.rmilk_status_success,
                        DateUtilities.getDateWithTimeFormat(MilkPreferences.this).
                        format(new Date(Utilities.getLastSyncDate())));
                statusColor = Color.rgb(0, 100, 0);
            }
            preference.setTitle(status);
            preference.setSummary(subtitle);
            final int statusColorSetting = statusColor;
            getListView().setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
                public void onChildViewRemoved(View arg0, View arg1) {
                    //
                }
                public void onChildViewAdded(View parent, View child) {
                    if(((ListView)parent).getChildCount() == 2) {
                        child.setBackgroundColor(statusColorSetting);
                    }
                }
            });
        }

        // sync button
        else if (r.getString(R.string.rmilk_MPr_sync_key).equals(preference.getKey())) {
            boolean loggedIn = Utilities.isLoggedIn();
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    new RTMSyncProvider().synchronize(MilkPreferences.this);
                    return true;
                }
            });
            if(!loggedIn)
                preference.setTitle(R.string.rmilk_MPr_sync_log_in);
        }

        // log out button
        else if (r.getString(R.string.rmilk_MPr_forget_key).equals(preference.getKey())) {
            boolean loggedIn = Utilities.isLoggedIn();
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