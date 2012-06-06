package com.todoroo.astrid.actfm;

import android.content.Intent;
import android.content.res.Resources;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
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
        if (!actFmPreferenceService.isLoggedIn()) {
            Intent intent = new Intent(this, ActFmLoginActivity.class);
            startActivityForResult(intent, REQUEST_LOGIN);
        } else {
            setResult(RESULT_CODE_SYNCHRONIZE);
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

        boolean loggedIn = getUtilities().isLoggedIn();
        PreferenceCategory status = (PreferenceCategory) findPreference(r.getString(R.string.sync_SPr_group_status));

        if (loggedIn)
            status.setTitle(getString(R.string.actfm_status_title_logged_in, actFmPreferenceService.getLoggedInUserName()));
        else
            status.setTitle(R.string.sync_SPr_group_status);

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
