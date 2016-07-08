package org.tasks;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;

import org.tasks.billing.InventoryHelper;
import org.tasks.billing.PurchaseHelper;
import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.TeslaUnreadReceiver;
import org.tasks.sync.SyncExecutor;

import javax.inject.Inject;

public class FlavorSetup {

    private final Preferences preferences;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final TeslaUnreadReceiver teslaUnreadReceiver;
    private final InventoryHelper inventoryHelper;
    private final SyncAdapterHelper syncAdapterHelper;

    @Inject
    public FlavorSetup(Preferences preferences, GtasksPreferenceService gtasksPreferenceService,
                       @SuppressWarnings("UnusedParameters") GtasksTaskListUpdater gtasksTaskListUpdater,
                       @SuppressWarnings("UnusedParameters") PurchaseHelper purchaseHelper,
                       @SuppressWarnings("UnusedParameters") SyncExecutor syncExecutor,
                       TeslaUnreadReceiver teslaUnreadReceiver, InventoryHelper inventoryHelper,
                       SyncAdapterHelper syncAdapterHelper) {
        this.preferences = preferences;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.teslaUnreadReceiver = teslaUnreadReceiver;
        this.inventoryHelper = inventoryHelper;
        this.syncAdapterHelper = syncAdapterHelper;
    }

    public void setup() {
        inventoryHelper.initialize();
        teslaUnreadReceiver.setEnabled(preferences.getBoolean(R.string.p_tesla_unread_enabled, false));
        gtasksPreferenceService.stopOngoing(); // if sync ongoing flag was set, clear it
        syncAdapterHelper.enableSynchronization(syncAdapterHelper.isEnabled());
    }
}
