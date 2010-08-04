package com.todoroo.astrid.producteev;

import android.content.SharedPreferences.Editor;

import com.timsu.astrid.R;
import com.todoroo.astrid.common.SyncProviderPreferences;
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
    public String getIdentifier() {
        return "pdv"; //$NON-NLS-1$
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_producteev;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.producteev_PPr_interval_key;
    }

    @Override
    public void startSync() {
        new ProducteevSyncProvider().synchronize(this);
    }

    @Override
    public void logOut() {
        new ProducteevSyncProvider().signOut();
    }

    // --- producteev-specific preferences

    private static final String PREF_SERVER_LAST_SYNC = "_last_server"; //$NON-NLS-1$

    /** @return last sync date, or null if no last */
    public String getLastServerSync() {
        return getPrefs().getString(getIdentifier() + PREF_SERVER_LAST_SYNC, null);
    }

    /** Deletes Last Successful Sync Date */
    public void setLastServerSync(String value) {
        Editor editor = getPrefs().edit();
        editor.putString(getIdentifier() + PREF_SERVER_LAST_SYNC, value);
        editor.commit();
    }


}