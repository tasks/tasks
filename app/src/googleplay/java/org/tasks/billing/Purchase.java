package org.tasks.billing;

import com.google.gson.GsonBuilder;

public class Purchase {

  private final com.android.billingclient.api.Purchase purchase;

  public Purchase(String json) {
    this(new GsonBuilder().create().fromJson(json, com.android.billingclient.api.Purchase.class));
  }

  public Purchase(com.android.billingclient.api.Purchase purchase) {
    this.purchase = purchase;
  }

  public String toJson() {
    return new GsonBuilder().create().toJson(purchase);
  }

  String getOriginalJson() {
    return purchase.getOriginalJson();
  }

  String getSignature() {
    return purchase.getSignature();
  }

  public String getSku() {
    return purchase.getSku();
  }

  String getPurchaseToken() {
    return purchase.getPurchaseToken();
  }

  boolean isIap() {
    return !SkuDetails.SKU_SUBS.contains(getSku());
  }

  @Override
  public String toString() {
    return "Purchase{" + "purchase=" + purchase + '}';
  }
}
