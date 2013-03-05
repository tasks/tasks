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
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.service.SyncResultCallbackWrapper.WidgetUpdatingCallbackWrapper;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;

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
            if (actFmPreferenceService.isLoggedIn()) {
                DialogUtilities.okCancelDialog(this, getString(R.string.DLG_warning), getString(R.string.gtasks_dual_sync_warning),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startLogin();
                            }
                        }, null);
            } else {
                startLogin();
            }
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
        //TODO: Implement me
        final ProgressDialog pd = DialogUtilities.progressDialog(this, "Importing Google Tasks");
        pd.setCancelable(false);
        pd.show();

        SyncResultCallback callback = new WidgetUpdatingCallbackWrapper(new SyncResultCallbackAdapter() {
            @Override
            public void finished() {
                super.finished();
                DialogUtilities.dismissDialog(GtasksPreferences.this, pd);
            }
        });

        GtasksSyncV2Provider.getInstance().synchronizeActiveTasks(true, callback);
    }

    private void startLogin() {
        Intent intent = new Intent(this, GtasksLoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    public void logOut() {
        GtasksSyncV2Provider.getInstance().signOut();
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
