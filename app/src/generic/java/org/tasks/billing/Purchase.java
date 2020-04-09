package org.tasks.billing;

public class Purchase {

  public Purchase(@SuppressWarnings("unused") String json) {}

  public String getSku() {
    return null;
  }

  public String toJson() {
    return null;
  }

  public boolean isCanceled() {
    return false;
  }

  public int getSubscriptionPrice() {
    return 0;
  }

  public boolean isMonthly() {
    return false;
  }

  boolean isProSubscription() {
    return false;
  }
}
