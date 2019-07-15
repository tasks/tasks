package org.tasks.billing;

import com.google.gson.GsonBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Purchase {

  private static final Pattern PATTERN = Pattern.compile("^(annual|monthly)_([0-1][0-9]|499)$");

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

  boolean isProSubscription() {
    return PATTERN.matcher(getSku()).matches();
  }

  boolean isMonthly() {
    return getSku().startsWith("monthly");
  }

  boolean isCanceled() {
    return !purchase.isAutoRenewing();
  }

  Integer getSubscriptionPrice() {
    Matcher matcher = PATTERN.matcher(getSku());
    if (matcher.matches()) {
      int price = Integer.parseInt(matcher.group(2));
      return price == 499 ? 5 : price;
    }
    return null;
  }

  @Override
  public String toString() {
    return "Purchase{" + "purchase=" + purchase + '}';
  }
}
