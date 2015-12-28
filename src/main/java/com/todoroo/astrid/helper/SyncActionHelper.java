/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import android.app.Activity;
import android.content.Intent;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.preferences.Preferences;
import org.tasks.sync.IndeterminateProgressBarSyncResultCallback;

/**
 * SyncActionHelper is a helper class for encapsulating UI actions
 * responsible for performing sync and prompting user to sign up for a new
 * sync service.
 * <p/>
 * In order to make this work you need to call register() and unregister() in
 * onResume and onPause, respectively.
 *
 * @author Tim Su <tim@astrid.com>
 */
public class SyncActionHelper {

    public static final String PREF_LAST_AUTO_SYNC = "taskListLastAutoSync"; //$NON-NLS-1$

    public final SyncResultCallback syncResultCallback;

    private final SyncV2Service syncService;
    private final Preferences preferences;

    // --- boilerplate

    public SyncActionHelper(GtasksPreferenceService gtasksPreferenceService, SyncV2Service syncService, final Activity activity, Preferences preferences) {
        this.syncService = syncService;
        this.preferences = preferences;
        syncResultCallback = new IndeterminateProgressBarSyncResultCallback(gtasksPreferenceService, activity, new Runnable() {
            @Override
            public void run() {
                activity.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
            }
        });
    }

    // --- automatic sync logic

    public void initiateAutomaticSync() {
        long tasksPushedAt = preferences.getLong(PREF_LAST_AUTO_SYNC, 0);
        if (DateUtilities.now() - tasksPushedAt > TaskListFragment.AUTOSYNC_INTERVAL) {
            performSyncServiceV2Sync();
        }
    }

    // --- sync logic

    protected void performSyncServiceV2Sync() {
        boolean syncOccurred = syncService.synchronizeActiveTasks(syncResultCallback);
        if (syncOccurred) {
            preferences.setLong(PREF_LAST_AUTO_SYNC, DateUtilities.now());
        }
    }

    public boolean performSyncAction() {
        if (syncService.isActive()) {
            syncService.synchronizeActiveTasks(syncResultCallback);
            return true;
        } else {
            return false;
        }
    }
}


