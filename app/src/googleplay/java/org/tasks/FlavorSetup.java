package org.tasks;

import javax.inject.Inject;
import org.tasks.billing.InventoryHelper;
import org.tasks.gtasks.PlayServices;

public class FlavorSetup {

  private final InventoryHelper inventoryHelper;
  private final PlayServices playServices;

  @Inject
  public FlavorSetup(InventoryHelper inventoryHelper, PlayServices playServices) {
    this.inventoryHelper = inventoryHelper;
    this.playServices = playServices;
  }

  public void setup() {
    inventoryHelper.initialize();
    playServices.refresh();
  }
}
