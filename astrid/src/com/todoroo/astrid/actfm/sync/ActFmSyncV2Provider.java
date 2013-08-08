/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import android.app.Activity;
import android.content.DialogInterface;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

import org.astrid.R;

import java.io.IOException;

/**
 * Exposes sync action
 */
public class ActFmSyncV2Provider extends SyncV2Provider {

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    @Autowired
    private Database database;

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    public String getName() {
        return ContextManager.getString(R.string.actfm_APr_header);
    }

    @Override
    public ActFmPreferenceService getUtilities() {
        return actFmPreferenceService;
    }

    @Override
    public void signOut(final Activity activity) {
        actFmPreferenceService.setToken(null);
        actFmPreferenceService.clearLastSyncDate();
        ActFmPreferenceService.premiumLogout();

        DialogUtilities.okCancelCustomDialog(activity,
                activity.getString(R.string.actfm_logout_clear_tasks_title),
                activity.getString(R.string.actfm_logout_clear_tasks_body),
                R.string.actfm_logout_clear_tasks_yes,
                R.string.actfm_logout_clear_tasks_no,
                android.R.drawable.ic_dialog_alert,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Preferences.clear(ActFmPreferenceService.PREF_USER_ID); // As though no one has ever logged in
                        activity.deleteDatabase(database.getName());
                        RemoteModelDao.setOutstandingEntryFlags(RemoteModelDao.OUTSTANDING_FLAG_UNINITIALIZED);
                        System.exit(0);
                    }
                },
                null);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    // --- synchronize active tasks

    @Override
    public void synchronizeActiveTasks(final boolean manual,
                                       final SyncResultCallback callback) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                updateUserStatus();

                ActFmSyncThread.getInstance().setTimeForBackgroundSync(true);
            }
        }).start();
    }

    /**
     * fetch user status hash
     */

    public void updateUserStatus() {
        try {
            throw new ActFmServiceException("not logged in", null);
        } catch (IOException e) {
            handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
        }
    }

    // --- synchronize list
    @Override
    public void synchronizeList(Object list, final boolean manual,
                                final SyncResultCallback callback) {
        // Nothing to do
    }
}
