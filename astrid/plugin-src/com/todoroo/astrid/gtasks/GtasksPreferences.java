package com.todoroo.astrid.gtasks;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncProvider;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksPreferences extends SyncProviderPreferences {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

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
        new GtasksSyncProvider().synchronize(this);
        finish();
    }

    @Override
    public void logOut() {
        new GtasksSyncProvider().signOut();
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

    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        if (r.getString(R.string.gtasks_GPr_sync_on_save_key).equals(preference.getKey())) {
            preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((Boolean) newValue).booleanValue()) {
                        startSync();
                    }
                    return true;
                }

            });
        } else {
            super.updatePreferences(preference, value);
        }
    }

}