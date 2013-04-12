/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;

import org.json.JSONObject;

import android.app.Activity;
import android.content.DialogInterface;

import com.facebook.Session;
import com.timsu.astrid.GCMIntentService;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.billing.BillingConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

/**
 * Exposes sync action
 *
 */
public class ActFmSyncV2Provider extends SyncV2Provider {

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    @Autowired
    private ActFmSyncService actFmSyncService;

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
        GCMIntentService.unregister(ContextManager.getContext());
        Session activeSession = Session.getActiveSession();
        if (activeSession != null) {
            activeSession.closeAndClearTokenInformation();
        }

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
                        ActFmSyncThread.clearTablePushedAtValues();
                        activity.deleteDatabase(database.getName());
                        RemoteModelDao.setOutstandingEntryFlags(RemoteModelDao.OUTSTANDING_FLAG_UNINITIALIZED);
                        System.exit(0);
                    }
                },
                null);
    }

    @Override
    public boolean isActive() {
        return actFmPreferenceService.isLoggedIn();
    }

    // --- synchronize active tasks

    @Override
    public void synchronizeActiveTasks(final boolean manual,
            final SyncResultCallback callback) {

        new Thread(new Runnable() {
            public void run() {

                updateUserStatus();

                ActFmSyncThread.getInstance().setTimeForBackgroundSync(true);
            }
        }).start();
    }

    /** fetch user status hash*/
    @SuppressWarnings("nls")
    public void updateUserStatus() {
        if (Preferences.getStringValue(GCMIntentService.PREF_NEEDS_REGISTRATION) != null) {
            actFmSyncService.setGCMRegistration(Preferences.getStringValue(GCMIntentService.PREF_NEEDS_REGISTRATION));
        } else if (Preferences.getBoolean(GCMIntentService.PREF_NEEDS_RETRY, false)) {
            GCMIntentService.register(ContextManager.getContext());
        }

        if (Preferences.getBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false)) {
            actFmSyncService.updateUserSubscriptionStatus(null, null, null);
        }

        try {
            JSONObject status = actFmSyncService.invoke("user_status"); //$NON-NLS-1$
            if (status.has("id"))
                Preferences.setString(ActFmPreferenceService.PREF_USER_ID, Long.toString(status.optLong("id")));
            if (status.has("name"))
                Preferences.setString(ActFmPreferenceService.PREF_NAME, status.optString("name"));
            if (status.has("first_name"))
                Preferences.setString(ActFmPreferenceService.PREF_FIRST_NAME, status.optString("first_name"));
            if (status.has("last_name"))
                Preferences.setString(ActFmPreferenceService.PREF_LAST_NAME, status.optString("last_name"));
            if (status.has("premium") && !Preferences.getBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false))
                Preferences.setBoolean(ActFmPreferenceService.PREF_PREMIUM, status.optBoolean("premium"));
            if (status.has("email"))
                Preferences.setString(ActFmPreferenceService.PREF_EMAIL, status.optString("email"));
            if (status.has("picture"))
                Preferences.setString(ActFmPreferenceService.PREF_PICTURE, status.optString("picture"));

            ActFmPreferenceService.reloadThisUser();
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
