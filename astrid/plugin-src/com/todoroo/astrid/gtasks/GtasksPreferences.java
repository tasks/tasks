/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider.GtasksImportCallback;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider.GtasksImportTuple;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;
import com.todoroo.astrid.tags.TagService;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksPreferences extends SyncProviderPreferences {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private ActFmPreferenceService actFmPreferenceService;
    @Autowired private TagService tagService;

    public GtasksPreferences() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_gtasks;
    }

    @Override
    public void startSync() {
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
        if (actFmPreferenceService.isLoggedIn()) {
            startBlockingImport();
        } else {
            setResultForSynchronize();
        }
    }

    private void setResultForSynchronize() {
        setResult(RESULT_CODE_SYNCHRONIZE);
        finish();
    }

    private void startBlockingImport() {
        final ProgressDialog pd = DialogUtilities.progressDialog(this, getString(R.string.gtasks_import_progress));
        pd.setCancelable(false);

        GtasksImportCallback callback = new GtasksImportCallback(new SyncResultCallbackAdapter() {/**/}) {
            @Override
            public void finished() {
                super.finished();
                for (GtasksImportTuple tuple : importConflicts) {
                    final GtasksImportTuple finalTuple = tuple;
                    String prompt = getString(R.string.gtasks_import_add_to_shared_list, tuple.tagName, tuple.taskName);
                    DialogUtilities.okCancelCustomDialog(GtasksPreferences.this,
                            getString(R.string.gtasks_import_dlg_title),
                            prompt,
                            R.string.gtasks_import_add_task_ok,
                            R.string.gtasks_import_add_task_cancel,
                            android.R.drawable.ic_dialog_alert,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Task task = new Task();
                                    task.setId(finalTuple.taskId);
                                    task.setUuid(finalTuple.taskUuid);
                                    tagService.createLink(task, finalTuple.tagName, finalTuple.tagUuid);
                                }
                            },
                            null);
                }
                DialogUtilities.dismissDialog(GtasksPreferences.this, pd);
            }
        };

        GtasksSyncV2Provider.getInstance().synchronizeActiveTasks(true, callback);
    }

    private void startLogin() {
        Intent intent = new Intent(this, GtasksLoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    public void logOut() {
        GtasksSyncV2Provider.getInstance().signOut(this);
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return gtasksPreferenceService;
    }

    @Override
    protected void onPause() {
        super.onPause();
        new GtasksBackgroundService().scheduleService();
    }
}
