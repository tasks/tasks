package org.tasks.billing;

import android.app.Activity;
import androidx.annotation.Nullable;

public interface BillingClient {
  void queryPurchases();

  void consume(String sku);

  void initiatePurchaseFlow(Activity activity, String sku, String skuType, @Nullable String oldSku);

  void addPurchaseCallback(OnPurchasesUpdated onPurchasesUpdated);
}
