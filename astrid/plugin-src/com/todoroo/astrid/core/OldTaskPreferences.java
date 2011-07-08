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
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.TodorooPreferenceActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;

/**
 * Displays the preference screen for users to edit their preferences
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class OldTaskPreferences extends TodorooPreferenceActivity {

    @Autowired private TaskService taskService;
    @Autowired private MetadataService metadataService;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_manage;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceScreen();
        DependencyInjectionService.getInstance().inject(this);

        // Extended prefs
        Preference extpreference_completed = screen.findPreference(getString(R.string.EPr_manage_delete_completed));
        extpreference_completed.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showDeleteCompletedDialog();
                return true;
            }
        });

        Preference extpreference_purged = screen.findPreference(getString(R.string.EPr_manage_purge_deleted));
        extpreference_purged.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                showPurgeDeletedDialog();
                return true;
            }
        });
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

    /** Run runnable with progress dialog */
    protected void runWithDialog(final Runnable runnable) {
        final ProgressDialog pd = DialogUtilities.progressDialog(this, getString(R.string.DLG_please_wait));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
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