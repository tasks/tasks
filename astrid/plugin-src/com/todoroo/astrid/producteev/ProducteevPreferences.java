package com.todoroo.astrid.producteev;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.common.SyncProviderPreferences;
import com.todoroo.astrid.common.SyncProviderUtilities;
import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class ProducteevPreferences extends SyncProviderPreferences {

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_producteev;
    }

    @Override
    public void startSync() {
        startService(new Intent(ProducteevBackgroundService.SYNC_ACTION, null,
                this, ProducteevBackgroundService.class));
    }

    @Override
    public void logOut() {
        new ProducteevSyncProvider().signOut();
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return ProducteevUtilities.INSTANCE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListPreference defaultDash = (ListPreference)findPreference(getString(R.string.producteev_PPr_defaultdash_key));
        if(ProducteevUtilities.INSTANCE.isLoggedIn()) {
            //
        }
        String[] entries = new String[2];
        entries[0] = getString(R.string.producteev_no_dashboard);
        entries[1] = getString(R.string.producteev_default_dashboard);

        String[] entryValues = new String[2];
        entryValues[0] = Integer.toString(ProducteevUtilities.DASHBOARD_NO_SYNC);
        entryValues[1] = Integer.toString(ProducteevUtilities.DASHBOARD_DEFAULT);
        defaultDash.setEntries(entries);
        defaultDash.setEntryValues(entryValues);
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        super.updatePreferences(preference, value);
        final Resources r = getResources();

        if (r.getString(R.string.producteev_PPr_defaultdash_key).equals(
                preference.getKey())) {
            int index = AndroidUtilities.indexOf(((ListPreference)preference).getEntryValues(), (String)value);
            if(index == -1)
                index = 1;
            if(index == 0)
                preference.setSummary(R.string.producteev_PPr_defaultdash_summary_none);
            else
                preference.setSummary(r.getString(
                        R.string.producteev_PPr_defaultdash_summary,
                        ((ListPreference)preference).getEntries()[index]));
        }
    }
}