package org.tasks.billing;

import android.app.Activity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import java.util.List;

public interface BillingClient {
  void queryPurchases();

  void querySkuDetails();

  void observeSkuDetails(
      LifecycleOwner owner,
      Observer<List<SkuDetails>> subscriptionObserver,
      Observer<List<SkuDetails>> iapObserver);

  int getErrorMessage();

  void consume(String sku);

  void initiatePurchaseFlow(Activity activity, String sku, String skuType);
}
