package com.todoroo.astrid.gtasks;

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
    public int getPreferenceResource() {
        return R.xml.preferences_gtasks;
    }

    @Override
    public void startSync() {
        new GtasksSyncProvider().synchronize(this);
    }

    @Override
    public void logOut() {
        new GtasksSyncProvider().signOut();
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return gtasksPreferenceService;
    }

}