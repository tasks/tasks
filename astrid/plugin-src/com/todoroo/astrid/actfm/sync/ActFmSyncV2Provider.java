/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import com.timsu.astrid.GCMIntentService;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.billing.BillingConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

/**
 * Exposes sync action
 *
 */
public class ActFmSyncV2Provider extends SyncV2Provider {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired TaskService taskService;

    @Autowired TagDataService tagDataService;

    @Autowired MetadataService metadataService;

    @Autowired UserDao userDao;

    @Autowired Database database;

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
    public void signOut() {
        actFmPreferenceService.setToken(null);
        actFmPreferenceService.clearLastSyncDate();
        ActFmPreferenceService.premiumLogout();
        GCMIntentService.unregister(ContextManager.getContext());
    }

    @Override
    public boolean isActive() {
        return actFmPreferenceService.isLoggedIn();
    }

    private static final String LAST_FEATURED_TAG_FETCH_TIME = "actfm_last_featuredTag"; //$NON-NLS-1$

    // --- synchronize active tasks

    @Override
    public void synchronizeActiveTasks(final boolean manual,
            final SyncResultCallback callback) {

        new Thread(new Runnable() {
            public void run() {

                final AtomicInteger finisher = new AtomicInteger(1);

                updateUserStatus();

                ActFmSyncThread.getInstance().setTimeForBackgroundSync(true);

                startFeaturedListFetcher(finisher);
            }
        }).start();
    }

    /** fetch user status hash*/
    @SuppressWarnings("nls")
    public void updateUserStatus() {
        if (Preferences.getStringValue(GCMIntentService.PREF_NEEDS_REGISTRATION) != null) {
            actFmSyncService.setGCMRegistration(Preferences.getStringValue(GCMIntentService.PREF_NEEDS_REGISTRATION));
        }

        if (Preferences.getBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false)) {
            actFmSyncService.updateUserSubscriptionStatus(null, null, null);
        }

        try {
            JSONObject status = actFmSyncService.invoke("user_status"); //$NON-NLS-1$
            if (status.has("id"))
                Preferences.setLong(ActFmPreferenceService.PREF_USER_ID, status.optLong("id"));
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

    /** fetch changes to tags */
    private void startFeaturedListFetcher(final AtomicInteger finisher) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int time = Preferences.getInt(LAST_FEATURED_TAG_FETCH_TIME, 0);
                try {
                    if (Preferences.getBoolean(R.string.p_show_featured_lists, false)) {
                        time = actFmSyncService.fetchFeaturedLists(time);
                        Preferences.setInt(LAST_FEATURED_TAG_FETCH_TIME, time);
                    }
                } catch (JSONException e) {
                    handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
                } catch (IOException e) {
                    handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
                } finally {
                    if(finisher.decrementAndGet() == 0) {
                        finishSync(null);
                    }
                }
            }
        }).start();
    }

    // --- synchronize list
    @Override
    public void synchronizeList(Object list, final boolean manual,
            final SyncResultCallback callback) {
        // Nothing to do
    }
}
