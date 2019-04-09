package org.tasks.billing;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;

import android.app.Activity;
import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse.RequestStatus;
import com.amazon.device.iap.model.UserDataResponse;
import java.util.List;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class BillingClientImpl implements BillingClient, PurchasingListener {

  private final MutableLiveData<List<SkuDetails>> skuDetails = new MutableLiveData<>();
  private final Inventory inventory;

  @Inject
  public BillingClientImpl(@ForApplication Context context, Inventory inventory, Tracker tracker) {
    this.inventory = inventory;
    PurchasingService.registerListener(context, this);
  }

  @Override
  public void observeSkuDetails(
      LifecycleOwner owner,
      Observer<List<SkuDetails>> subscriptionObserver,
      Observer<List<SkuDetails>> iapObserver) {
    skuDetails.observe(owner, subscriptionObserver);
  }

  @Override
  public void queryPurchases() {
    PurchasingService.getPurchaseUpdates(true);
  }

  @Override
  public void querySkuDetails() {
    PurchasingService.getProductData(newHashSet(SkuDetails.SKU_PRO));
  }

  @Override
  public void consume(String sku) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void initiatePurchaseFlow(Activity activity, String sku, String skuType) {
    PurchasingService.purchase(sku);
  }

  @Override
  public int getErrorMessage() {
    return 0;
  }

  @Override
  public void onUserDataResponse(UserDataResponse userDataResponse) {
    Timber.d("onUserDataResponse(%s)", userDataResponse);
  }

  @Override
  public void onProductDataResponse(ProductDataResponse productDataResponse) {
    Timber.d("onProductDataResponse(%s)", productDataResponse);
    if (productDataResponse.getRequestStatus() == ProductDataResponse.RequestStatus.SUCCESSFUL) {
      skuDetails.setValue(
          newArrayList(transform(productDataResponse.getProductData().values(), SkuDetails::new)));
    }
  }

  @Override
  public void onPurchaseResponse(PurchaseResponse purchaseResponse) {
    Timber.d("onPurchaseResponse(%s)", purchaseResponse);
    if (purchaseResponse.getRequestStatus() == PurchaseResponse.RequestStatus.SUCCESSFUL) {
      inventory.add(new Purchase(purchaseResponse.getReceipt()));
      PurchasingService.notifyFulfillment(
          purchaseResponse.getReceipt().getReceiptId(), FulfillmentResult.FULFILLED);
    }
  }

  @Override
  public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {
    Timber.d("onPurchaseUpdatesResponse(%s)", purchaseUpdatesResponse);
    if (purchaseUpdatesResponse.getRequestStatus() == RequestStatus.SUCCESSFUL) {
      inventory.clear();
      inventory.add(transform(purchaseUpdatesResponse.getReceipts(), Purchase::new));
    }
  }
}
