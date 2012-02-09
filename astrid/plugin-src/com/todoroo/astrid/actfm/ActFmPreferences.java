package com.todoroo.astrid.actfm;

import android.content.Intent;
import android.content.res.Resources;
import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class ActFmPreferences extends SyncProviderPreferences {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_actfm;
    }

    @Override
    public void startSync() {
        if (!actFmPreferenceService.isLoggedIn()) {
            Intent intent = new Intent(this, ActFmLoginActivity.class);
            startActivityForResult(intent, 0);
        } else {
            new ActFmSyncV2Provider().synchronizeActiveTasks(true, new SyncResultCallbackAdapter() {
                @Override
                public void finished() {
                    ContextManager.getContext().sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                }
            });
            finish();
        }
    }

    @Override
    public void logOut() {
        new ActFmSyncV2Provider().signOut();
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return actFmPreferenceService;
    }

    @Override
    protected void onPause() {
        super.onPause();
        new ActFmBackgroundService().scheduleService();
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        if (r.getString(R.string.actfm_https_key).equals(preference.getKey())) {
            if ((Boolean)value)
                preference.setSummary(R.string.actfm_https_enabled);
            else
                preference.setSummary(R.string.actfm_https_disabled);
        } else {
            super.updatePreferences(preference, value);
        }
    }

}