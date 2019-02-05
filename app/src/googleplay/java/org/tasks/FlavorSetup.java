package org.tasks;

import javax.inject.Inject;
import org.tasks.billing.BillingClient;

public class FlavorSetup {

  private final BillingClient billingClient;

  @Inject
  public FlavorSetup(BillingClient billingClient) {
    this.billingClient = billingClient;
  }

  public void setup() {
    billingClient.initialize();
  }
}
