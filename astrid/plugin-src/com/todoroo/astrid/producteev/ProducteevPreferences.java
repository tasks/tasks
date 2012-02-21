package com.todoroo.astrid.producteev;

import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

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
        new ProducteevSyncProvider().synchronize(this);
        finish();
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
    protected void onPause() {
        super.onPause();
        new ProducteevBackgroundService().scheduleService();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListPreference defaultDash = (ListPreference)findPreference(getString(R.string.producteev_PPr_defaultdash_key));
        String[] entries, entryValues;
        StoreObject[] dashboards = ProducteevDataService.getInstance().getDashboards();
        if(ProducteevUtilities.INSTANCE.isLoggedIn() && dashboards.length > 0) {
            entries = new String[dashboards.length + 1];
            entryValues = new String[dashboards.length + 1];
            for(int i = 0; i < dashboards.length; i++) {
                entries[i + 1] = dashboards[i].getValue(ProducteevDashboard.NAME);
                entryValues[i + 1] = Long.toString(dashboards[i].getValue(ProducteevDashboard.REMOTE_ID));
            }
        } else {
            entries = new String[2];
            entries[1] = getString(R.string.producteev_default_dashboard);
            entryValues = new String[2];
            entryValues[1] = Integer.toString(ProducteevUtilities.DASHBOARD_DEFAULT);
        }
        entries[0] = getString(R.string.producteev_no_dashboard);
        entryValues[0] = Integer.toString(ProducteevUtilities.DASHBOARD_NO_SYNC);
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
