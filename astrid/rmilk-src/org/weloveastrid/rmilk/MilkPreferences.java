package org.weloveastrid.rmilk;

import org.weloveastrid.rmilk.sync.MilkSyncProvider;

import com.timsu.astrid.R;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkPreferences extends SyncProviderPreferences {

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_rmilk;
    }

    @Override
    public void startSync() {
        new MilkSyncProvider().synchronize(this);
        finish();
    }

    @Override
    public void logOut() {
        new MilkSyncProvider().signOut(this);
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return MilkUtilities.INSTANCE;
    }

}
