/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;

/**
 * Displays the preference screen for users to manage their old tasks and events
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OldTaskPreferences extends TodorooPreferenceActivity {

    @Autowired private TaskService taskService;
    @Autowired private MetadataService metadataService;

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
        Preference preference_delete_completed = screen.findPreference(getString(R.string.EPr_manage_delete_completed));
        preference_delete_completed.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showDeleteCompletedDialog();
                return true;
            }
        });

        Preference preference_purge_deleted = screen.findPreference(getString(R.string.EPr_manage_purge_deleted));
        preference_purge_deleted.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showPurgeDeletedDialog();
                return true;
            }
        });

        Preference preference_delete_completed_events = screen.findPreference(getString(R.string.EPr_manage_delete_completed_gcal));
        preference_delete_completed_events.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showDeleteCompletedEventsDialog();
                return true;
            }
        });

        Preference preference_delete_all_events = screen.findPreference(getString(R.string.EPr_manage_delete_all_gcal));
        preference_delete_all_events.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showDeleteAllEventsDialog();
                return true;
            }
        });
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
                        runWithDialog(new Runnable() {
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
                        runWithDialog(new Runnable() {
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
                        runWithDialog(new Runnable() {
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
                                template.setValue(Task.CALENDAR_URI, "");
                                int result = taskService.update(
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
                        runWithDialog(new Runnable() {
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
                                template.setValue(Task.CALENDAR_URI, "");
                                int result = taskService.update(
                                        Task.CALENDAR_URI.isNotNull(),
                                        template);
                                showResult(R.string.EPr_manage_delete_all_gcal_status, deletedEventCount);
                            }
                        });
                    }
                }, null);
    }

    /** Run runnable with progress dialog */
    protected void runWithDialog(final Runnable runnable) {
        pd = DialogUtilities.progressDialog(this, getString(R.string.DLG_please_wait));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    DialogUtilities.okDialog(OldTaskPreferences.this,
                            getString(R.string.DLG_error, e.toString()), null);
                } finally {
                    DialogUtilities.dismissDialog(OldTaskPreferences.this, pd);
                }
            }
        }).start();
    }

    protected void showResult(int resourceText, int result) {
        DialogUtilities.okDialog(this, getString(resourceText, result), null);
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        // :)
    }

}