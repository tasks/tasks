package org.tasks;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.billing.InventoryHelper;
import org.tasks.gtasks.PlayServicesAvailability;
import org.tasks.gtasks.SyncAdapterHelper;

import javax.inject.Inject;

public class FlavorSetup {

    private final GtasksPreferenceService gtasksPreferenceService;
    private final InventoryHelper inventoryHelper;
    private final SyncAdapterHelper syncAdapterHelper;
    private final PlayServicesAvailability playServicesAvailability;

    @Inject
    public FlavorSetup(GtasksPreferenceService gtasksPreferenceService, InventoryHelper inventoryHelper,
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
