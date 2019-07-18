package org.tasks.billing;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import java.util.List;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;

public class BillingClientImpl implements BillingClient {
  @Inject
  public BillingClientImpl(Context context, Inventory inventory, Tracker tracker) {}

  @Override
  public void queryPurchases() {}

  @Override
  public int getErrorMessage() {
    return 0;
  }

  @Override
  public void initiatePurchaseFlow(
      Activity activity, String sku, String skuType, @Nullable String oldSku) {}

  @Override
  public void addPurchaseCallback(OnPurchasesUpdated onPurchasesUpdated) {

  }

  @Override
  public void querySkuDetails() {}

  @Override
  public void observeSkuDetails(
      LifecycleOwner owner,
      Observer<List<SkuDetails>> subscriptionObserver,
      Observer<List<SkuDetails>> iapObserver) {}

  @Override
  public void consume(String sku) {}
}
