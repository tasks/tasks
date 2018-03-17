package org.tasks;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.billing.InventoryHelper;
import org.tasks.gtasks.GtaskSyncAdapterHelper;
import org.tasks.gtasks.PlayServices;

import javax.inject.Inject;

public class FlavorSetup {

    private final GtasksPreferenceService gtasksPreferenceService;
    private final InventoryHelper inventoryHelper;
    private final GtaskSyncAdapterHelper gtaskSyncAdapterHelper;
    private final PlayServices playServices;

    @Inject
    public FlavorSetup(GtasksPreferenceService gtasksPreferenceService, InventoryHelper inventoryHelper,
                       GtaskSyncAdapterHelper gtaskSyncAdapterHelper, PlayServices playServices) {
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.inventoryHelper = inventoryHelper;
        this.gtaskSyncAdapterHelper = gtaskSyncAdapterHelper;
        this.playServices = playServices;
    }

    public void setup() {
        inventoryHelper.initialize();
        gtasksPreferenceService.stopOngoing(); // if sync ongoing flag was set, clear it
        gtaskSyncAdapterHelper.enableBackgroundSynchronization(gtaskSyncAdapterHelper.isBackgroundSyncEnabled());
        playServices.refresh();
    }
}
