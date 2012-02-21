package com.todoroo.astrid.gtasks;

import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;

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
        if (!gtasksPreferenceService.isLoggedIn()) {
            Intent intent = new Intent(this, GtasksLoginActivity.class);
            startActivityForResult(intent, 0);
        } else {
            new GtasksSyncV2Provider().synchronizeActiveTasks(true, new SyncResultCallbackAdapter() {
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
        new GtasksSyncV2Provider().signOut();
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
}
