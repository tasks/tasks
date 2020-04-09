package org.tasks.billing;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import org.tasks.analytics.Tracker;

public class BillingClientImpl implements BillingClient {

  public static final String TYPE_SUBS = "";

  public BillingClientImpl(Context context, Inventory inventory, Tracker tracker) {}

  @Override
  public void queryPurchases() {}

  @Override
  public void initiatePurchaseFlow(
      Activity activity, String sku, String skuType, @Nullable String oldSku) {}

  @Override
  public void addPurchaseCallback(OnPurchasesUpdated onPurchasesUpdated) {}

  @Override
  public void consume(String sku) {}
}
