package com.todoroo.astrid.backup;

import java.util.Date;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.andlib.widget.TodorooPreferences;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class BackupPreferences extends TodorooPreferences {

    static final String PREF_BACKUP_LAST_DATE = "backupDate"; //$NON-NLS-1$

    static final String PREF_BACKUP_LAST_ERROR = "backupError"; //$NON-NLS-1$

    private int statusColor = Color.BLACK;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_backup;
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
        BackupService.scheduleService(this);
    }

    /**
     *
     * @param resource
     *            if null, updates all resources
     */
    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        // auto
        if (r.getString(R.string.backup_BPr_auto_key).equals(
                preference.getKey())) {
            if (value != null && !(Boolean)value)
                preference.setSummary(R.string.backup_BPr_auto_disabled);
            else
                preference.setSummary(R.string.backup_BPr_auto_enabled);
        }

        // status
        else if (r.getString(R.string.backup_BPr_status_key).equals(preference.getKey())) {
            String status;
            String subtitle = ""; //$NON-NLS-1$

            // last backup was error
            final long last = Preferences.getLong(PREF_BACKUP_LAST_DATE, 0);
            final String error = Preferences.getStringValue(PREF_BACKUP_LAST_ERROR);
            if(error != null) {
                status = r.getString(R.string.backup_status_failed);
                subtitle = r.getString(R.string.backup_status_failed_subtitle);
                statusColor = Color.rgb(100, 0, 0);
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        DialogUtilities.okDialog(BackupPreferences.this, error, null);
                        return true;
                    }
                });
            } else if(last > 0) {
                status = r.getString(R.string.backup_status_success,
                        DateUtilities.getDateStringWithTime(BackupPreferences.this,
                        new Date(last)));
                statusColor = Color.rgb(0, 100, 0);
                preference.setOnPreferenceClickListener(null);
            } else {
                status = r.getString(R.string.backup_status_never);
                statusColor = Color.rgb(0, 0, 100);
                preference.setOnPreferenceClickListener(null);
            }
            preference.setTitle(status);
            preference.setSummary(subtitle);

            View view = findViewById(R.id.status);
            if(view != null)
                view.setBackgroundColor(statusColor);
        }

    }

}