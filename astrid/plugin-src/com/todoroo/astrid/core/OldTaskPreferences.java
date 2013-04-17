/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.facebook.Session;
import com.timsu.astrid.GCMIntentService;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.AstridPreferences;

/**
 * Displays the preference screen for users to manage their old tasks and events
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OldTaskPreferences extends TodorooPreferenceActivity {

    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;
    @Autowired Database database;

    @Autowired ActFmSyncService actFmSyncService;

    ProgressDialog pd;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_oldtasks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceScreen();
        DependencyInjectionService.getInstance().inject(this);

        // Extended prefs
        Preference preference = screen.findPreference(getString(R.string.EPr_manage_delete_completed));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showDeleteCompletedDialog();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.EPr_reset_sync_state));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                showResetSyncStateDialog();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.EPr_manage_purge_deleted));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showPurgeDeletedDialog();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.EPr_manage_delete_completed_gcal));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showDeleteCompletedEventsDialog();
                return true;
            }
        });

        preference = screen.findPreference(getString(R.string.EPr_manage_delete_all_gcal));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showDeleteAllEventsDialog();
                return true;
            }
        });

        preference= screen.findPreference(getString(R.string.EPr_manage_clear_all));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showClearDataDialog();
                return true;
            }
        });
    }

    private void showClearDataDialog() {
        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(
                        R.string.EPr_manage_clear_all_message),
                        new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Editor editor = Preferences.getPrefs(OldTaskPreferences.this).edit();
                        editor.clear();
                        editor.commit();

                        deleteDatabase(database.getName());
                        Session activeSession = Session.getActiveSession();
                        if (activeSession != null) {
                            activeSession.closeAndClearTokenInformation();
                        }

                        AstridPreferences.setPreferenceDefaults();
                        GCMIntentService.unregister(OldTaskPreferences.this);

                        System.exit(0);
                    }
                },
                null);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        DialogUtilities.dismissDialog(this, pd);

        super.onPause();
    }

    /** Show the dialog to delete completed tasks */
    private void showDeleteCompletedDialog() {
        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(
                        R.string.EPr_manage_delete_completed_message),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pd = DialogUtilities.runWithProgressDialog(OldTaskPreferences.this, new Runnable() {
                            @Override
                            public void run() {
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.COMPLETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    Task task = new Task();
                                    int length = cursor.getCount();
                                    for(int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        task.readFromCursor(cursor);
                                        GCalHelper.deleteTaskEvent(task);
                                    }
                                } finally {
                                    cursor.close();
                                }
                                Task template = new Task();
                                template.setValue(Task.DELETION_DATE,
                                        DateUtilities.now());
                                int result = taskService.update(
                                        Task.COMPLETION_DATE.gt(0), template);
                                showResult(
                                        R.string.EPr_manage_delete_completed_status,
                                        result);
                            }
                        });
                    }
                }, null);
    }

    /** Show the dialog to purge deleted tasks */
    private void showPurgeDeletedDialog() {
        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(
                        R.string.EPr_manage_purge_deleted_message),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pd = DialogUtilities.runWithProgressDialog(OldTaskPreferences.this, new Runnable() {
                            @Override
                            public void run() {
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.DELETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    Task task = new Task();
                                    int length = cursor.getCount();
                                    for(int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        task.readFromCursor(cursor);
                                        GCalHelper.deleteTaskEvent(task);
                                    }
                                } finally {
                                    cursor.close();
                                }
                                int result = taskService.deleteWhere(Task.DELETION_DATE.gt(0));
                                metadataService.cleanup();
                                showResult(
                                        R.string.EPr_manage_purge_deleted_status,
                                        result);
                            }
                        });
                    }
                }, null);
    }

    /** Show the dialog to delete completed events */
    private void showDeleteCompletedEventsDialog() {
        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(
                        R.string.EPr_manage_delete_completed_gcal_message),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pd = DialogUtilities.runWithProgressDialog(OldTaskPreferences.this, new Runnable() {
                            @Override
                            public void run() {
                                int deletedEventCount = 0;
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.COMPLETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    Task task = new Task();
                                    int length = cursor.getCount();
                                    for(int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        task.readFromCursor(cursor);
                                        if (GCalHelper.deleteTaskEvent(task))
                                            deletedEventCount++;
                                    }
                                } finally {
                                    cursor.close();
                                }
                                // mass update the CALENDAR_URI here,
                                // since the GCalHelper doesnt save it due to performance-reasons
                                Task template = new Task();
                                template.setValue(Task.CALENDAR_URI, ""); //$NON-NLS-1$
                                taskService.update(
                                        Criterion.and(Task.COMPLETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull()),
                                        template);
                                showResult(R.string.EPr_manage_delete_completed_gcal_status, deletedEventCount);
                            }
                        });
                    }
                }, null);
    }

    /** Show the dialog to delete all events */
    private void showDeleteAllEventsDialog() {
        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(
                        R.string.EPr_manage_delete_all_gcal_message),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pd = DialogUtilities.runWithProgressDialog(OldTaskPreferences.this, new Runnable() {
                            @Override
                            public void run() {
                                int deletedEventCount = 0;
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.CALENDAR_URI).where(
                                        Task.CALENDAR_URI.isNotNull()));
                                try {
                                    Task task = new Task();
                                    int length = cursor.getCount();
                                    for(int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        task.readFromCursor(cursor);
                                        if (GCalHelper.deleteTaskEvent(task))
                                            deletedEventCount++;
                                    }
                                } finally {
                                    cursor.close();
                                }
                                // mass update the CALENDAR_URI here,
                                // since the GCalHelper doesnt save it due to performance-reasons
                                Task template = new Task();
                                template.setValue(Task.CALENDAR_URI, ""); //$NON-NLS-1$
                                taskService.update(
                                        Task.CALENDAR_URI.isNotNull(),
                                        template);
                                showResult(R.string.EPr_manage_delete_all_gcal_status, deletedEventCount);
                            }
                        });
                    }
                }, null);
    }

    private void showResetSyncStateDialog() {
        DialogUtilities.okCancelDialog(this, getString(R.string.EPr_reset_sync_state_detailed), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActFmSyncThread.clearTablePushedAtValues();
            }
        }, null);
    }

    protected void showResult(int resourceText, int result) {
        DialogUtilities.okDialog(this, getString(resourceText, result), null);
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        // :)
    }

}
