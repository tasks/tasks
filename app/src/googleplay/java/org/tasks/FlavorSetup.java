package org.tasks;

import javax.inject.Inject;
import org.tasks.billing.BillingClient;
import org.tasks.gtasks.PlayServices;

public class FlavorSetup {

  private final PlayServices playServices;
  private final BillingClient billingClient;

  @Inject
  public FlavorSetup(PlayServices playServices, BillingClient billingClient) {
    this.playServices = playServices;
    this.billingClient = billingClient;
  }

  public void setup() {
    billingClient.initialize();
    playServices.refresh();
  }
}
