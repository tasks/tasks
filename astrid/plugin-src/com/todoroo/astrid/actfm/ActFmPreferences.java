package com.todoroo.astrid.actfm;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncProvider;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

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
        new ActFmSyncProvider().synchronize(this);
        finish();
    }

    @Override
    public void logOut() {
        new ActFmSyncProvider().signOut();
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

}