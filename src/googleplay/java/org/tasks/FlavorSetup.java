package org.tasks;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;

import org.tasks.billing.InventoryHelper;
import org.tasks.billing.PurchaseHelper;
import org.tasks.gtasks.PlayServicesAvailability;
import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.sync.SyncExecutor;

import javax.inject.Inject;

public class FlavorSetup {

    private final GtasksPreferenceService gtasksPreferenceService;
    private final InventoryHelper inventoryHelper;
    private final SyncAdapterHelper syncAdapterHelper;
    private final PlayServicesAvailability playServicesAvailability;

    @Inject
    public FlavorSetup(GtasksPreferenceService gtasksPreferenceService,
                       @SuppressWarnings("UnusedParameters") GtasksTaskListUpdater gtasksTaskListUpdater,
                       @SuppressWarnings("UnusedParameters") PurchaseHelper purchaseHelper,
                       @SuppressWarnings("UnusedParameters") SyncExecutor syncExecutor,
                       InventoryHelper inventoryHelper,
                       SyncAdapterHelper syncAdapterHelper, PlayServicesAvailability playServicesAvailability) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.inventoryHelper = inventoryHelper;
        this.syncAdapterHelper = syncAdapterHelper;
        this.playServicesAvailability = playServicesAvailability;
    }

    public void setup() {
        inventoryHelper.initialize();
        gtasksPreferenceService.stopOngoing(); // if sync ongoing flag was set, clear it
        syncAdapterHelper.enableSynchronization(syncAdapterHelper.isEnabled());
        playServicesAvailability.refresh();
    }
}
