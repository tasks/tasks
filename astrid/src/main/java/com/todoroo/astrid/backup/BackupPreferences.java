/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.utility.Flags;
import com.todoroo.astrid.utility.TodorooPreferenceActivity;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class BackupPreferences extends TodorooPreferenceActivity {

    static final String PREF_BACKUP_LAST_DATE = "backupDate"; //$NON-NLS-1$

    static final String PREF_BACKUP_LAST_ERROR = "backupError"; //$NON-NLS-1$

    private int statusColor = Color.BLACK;

    @Inject Preferences preferences;
    @Inject TasksXmlImporter xmlImporter;
    @Inject TasksXmlExporter xmlExporter;

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
                if(view != null) {
                    view.setBackgroundColor(statusColor);
                }
            }
        });

        findPreference(getString(R.string.backup_BAc_import)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                importTasks();
                return true;
            }
        });

        findPreference(getString(R.string.backup_BAc_export)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                xmlExporter.exportTasks(BackupPreferences.this, TasksXmlExporter.ExportType.EXPORT_TYPE_MANUAL, null);
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        BackupService.scheduleService(preferences, this);
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        // auto
        if (r.getString(R.string.backup_BPr_auto_key).equals(
                preference.getKey())) {
            if (value != null && !(Boolean)value) {
                preference.setSummary(R.string.backup_BPr_auto_disabled);
            } else {
                preference.setSummary(R.string.backup_BPr_auto_enabled);
            }
        }

        // status
        else if (r.getString(R.string.backup_BPr_status_key).equals(preference.getKey())) {
            String status;
            String subtitle = ""; //$NON-NLS-1$

            // last backup was error
            final long last = preferences.getLong(PREF_BACKUP_LAST_DATE, 0);
            final String error = preferences.getStringValue(PREF_BACKUP_LAST_ERROR);
            if(error != null) {
                status = r.getString(R.string.backup_status_failed);
                subtitle = r.getString(R.string.backup_status_failed_subtitle);
                statusColor = Color.rgb(100, 0, 0);
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference p) {
                        DialogUtilities.okDialog(BackupPreferences.this, error, null);
                        return true;
                    }
                });
            } else if(last > 0) {
                status = r.getString(R.string.backup_status_success,
                        DateUtilities.getDateStringWithTime(BackupPreferences.this,
                        newDate(last)));
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
            if(view != null) {
                view.setBackgroundColor(statusColor);
            }
        }
    }

    private void importTasks() {
        FilePickerBuilder.OnFilePickedListener listener = new FilePickerBuilder.OnFilePickedListener() {
            @Override
            public void onFilePicked(String filePath) {
                xmlImporter.importTasks(BackupPreferences.this, filePath, new Runnable() {
                    @Override
                    public void run() {
                        Flags.set(Flags.REFRESH);
                    }
                });
            }
        };
        new FilePickerBuilder(this,
                getString(R.string.import_file_prompt),
                BackupConstants.defaultExportDirectory(),
                listener).show();
    }
}
