package com.todoroo.astrid.producteev;

import com.timsu.astrid.R;
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
        new ProducteevSyncProvider().synchronize(this);
    }

    @Override
    public void logOut() {
        new ProducteevSyncProvider().signOut();
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return ProducteevUtilities.INSTANCE;
    }


}