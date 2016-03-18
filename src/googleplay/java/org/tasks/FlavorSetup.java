package org.tasks;

import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;

import org.tasks.billing.InventoryHelper;
import org.tasks.billing.PurchaseHelper;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.TeslaUnreadReceiver;

import javax.inject.Inject;

public class FlavorSetup {

    private final Preferences preferences;
    private final GtasksSyncService gtasksSyncService;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final TeslaUnreadReceiver teslaUnreadReceiver;
    private final InventoryHelper inventoryHelper;

    @Inject
    public FlavorSetup(Preferences preferences,
                       @SuppressWarnings("UnusedParameters") GtasksTaskListUpdater gtasksTaskListUpdater,
                       @SuppressWarnings("UnusedParameters") PurchaseHelper purchaseHelper,
                       GtasksSyncService gtasksSyncService, GtasksPreferenceService gtasksPreferenceService,
                       TeslaUnreadReceiver teslaUnreadReceiver, InventoryHelper inventoryHelper) {
        this.preferences = preferences;
        this.gtasksSyncService = gtasksSyncService;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.teslaUnreadReceiver = teslaUnreadReceiver;
        this.inventoryHelper = inventoryHelper;
    }

    public void setup() {
        inventoryHelper.initialize();
        teslaUnreadReceiver.setEnabled(preferences.getBoolean(R.string.p_tesla_unread_enabled, false));
        gtasksPreferenceService.stopOngoing(); // if sync ongoing flag was set, clear it
        gtasksSyncService.initialize();
    }
}
