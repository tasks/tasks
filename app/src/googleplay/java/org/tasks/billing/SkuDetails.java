package org.tasks.billing;

import com.android.billingclient.api.BillingClient.SkuType;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class SkuDetails {

  static final String SKU_PRO = "annual_499";
  static final List<String> SKU_SUBS = ImmutableList.of(SKU_PRO);

  static final String TYPE_INAPP = SkuType.INAPP;
  static final String TYPE_SUBS = SkuType.SUBS;

  private final com.android.billingclient.api.SkuDetails skuDetails;

  SkuDetails(com.android.billingclient.api.SkuDetails skuDetails) {
    this.skuDetails = skuDetails;
  }

  public String getSku() {
    return skuDetails.getSku();
  }

  public String getTitle() {
    return skuDetails.getTitle();
  }

  public String getPrice() {
    return skuDetails.getPrice();
  }

  public String getDescription() {
    return skuDetails.getDescription();
  }

  public String getSkuType() {
    return skuDetails.getType();
  }
}
