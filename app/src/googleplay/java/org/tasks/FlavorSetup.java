package org.tasks;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.billing.InventoryHelper;
import org.tasks.gtasks.PlayServicesAvailability;
import org.tasks.gtasks.GtaskSyncAdapterHelper;

import javax.inject.Inject;

public class FlavorSetup {

    private final GtasksPreferenceService gtasksPreferenceService;
    private final InventoryHelper inventoryHelper;
    private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
    private final PlayServicesAvailability playServicesAvailability;

    @Inject
    public FlavorSetup(GtasksPreferenceService gtasksPreferenceService, InventoryHelper inventoryHelper,
                       GtaskSyncAdapterHelper gtaskSyncAdapterHelper, PlayServicesAvailability playServicesAvailability) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.inventoryHelper = inventoryHelper;
        this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
        this.playServicesAvailability = playServicesAvailability;
    }

    public void setup() {
        inventoryHelper.initialize();
        gtasksPreferenceService.stopOngoing(); // if sync ongoing flag was set, clear it
        gtaskSyncAdapterHelper.enableSynchronization(gtaskSyncAdapterHelper.isEnabled());
        playServicesAvailability.refresh();
    }
}
