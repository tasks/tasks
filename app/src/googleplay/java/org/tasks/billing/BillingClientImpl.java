package org.tasks.billing;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;
import static org.tasks.billing.Inventory.SKU_DASHCLOCK;
import static org.tasks.billing.Inventory.SKU_TASKER;
import static org.tasks.billing.Inventory.SKU_THEMES;
import static org.tasks.billing.Inventory.SKU_VIP;

import android.app.Activity;
import android.content.Context;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

@SuppressWarnings("all")
public class BillingClientImpl implements BillingClient, PurchasesUpdatedListener {

  private static final List<String> DEBUG_SKUS =
      ImmutableList.of(SKU_THEMES, SKU_TASKER, SKU_DASHCLOCK, SKU_VIP);

  private final MutableLiveData<List<SkuDetails>> skuDetails = new MutableLiveData<>();
  private final Inventory inventory;
  private final Tracker tracker;
  MutableLiveData<List<SkuDetails>> subscriptions = new MutableLiveData<>();
  MutableLiveData<List<SkuDetails>> iaps = new MutableLiveData<>();
  private com.android.billingclient.api.BillingClient billingClient;
  private boolean connected;
  private int billingClientResponseCode = -1;

  @Inject
  public BillingClientImpl(@ForApplication Context context, Inventory inventory, Tracker tracker) {
    this.inventory = inventory;
    this.tracker = tracker;
    billingClient =
        com.android.billingclient.api.BillingClient.newBuilder(context).setListener(this).build();
  }

  public static String BillingResponseToString(@BillingResponse int response) {
    switch (response) {
      case BillingResponse.FEATURE_NOT_SUPPORTED:
        return "FEATURE_NOT_SUPPORTED";
      case BillingResponse.SERVICE_DISCONNECTED:
        return "SERVICE_DISCONNECTED";
      case BillingResponse.OK:
        return "OK";
      case BillingResponse.USER_CANCELED:
        return "USER_CANCELED";
      case BillingResponse.SERVICE_UNAVAILABLE:
        return "SERVICE_UNAVAILABLE";
      case BillingResponse.BILLING_UNAVAILABLE:
        return "BILLING_UNAVAILABLE";
      case BillingResponse.ITEM_UNAVAILABLE:
        return "ITEM_UNAVAILABLE";
      case BillingResponse.DEVELOPER_ERROR:
        return "DEVELOPER_ERROR";
      case BillingResponse.ERROR:
        return "ERROR";
      case BillingResponse.ITEM_ALREADY_OWNED:
        return "ITEM_ALREADY_OWNED";
      case BillingResponse.ITEM_NOT_OWNED:
        return "ITEM_NOT_OWNED";
      default:
        return "Unknown";
    }
  }

  /**
   * Query purchases across various use cases and deliver the result in a formalized way through a
   * listener
   */
  @Override
  public void queryPurchases() {
    Runnable queryToExecute =
        () -> {
          Single<PurchasesResult> purchases =
              Single.fromCallable(() -> billingClient.queryPurchases(SkuType.INAPP));
          if (areSubscriptionsSupported()) {
            purchases =
                Single.zip(
                    purchases,
                    Single.fromCallable(() -> billingClient.queryPurchases(SkuType.SUBS)),
                    (iaps, subs) -> {
                      if (iaps.getResponseCode() != BillingResponse.OK) {
                        return iaps;
                      }
                      if (subs.getResponseCode() != BillingResponse.OK) {
                        return subs;
                      }
                      iaps.getPurchasesList().addAll(subs.getPurchasesList());
                      return iaps;
                    });
          }
          purchases
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(this::onQueryPurchasesFinished);
        };

    executeServiceRequest(queryToExecute);
  }

  /** Handle a result from querying of purchases and report an updated list to the listener */
  private void onQueryPurchasesFinished(PurchasesResult result) {
    assertMainThread();

    // Have we been disposed of in the meantime? If so, or bad result code, then quit
    if (billingClient == null || result.getResponseCode() != BillingResponse.OK) {
      Timber.w(
          "Billing client was null or result code (%s) was bad - quitting",
          result.getResponseCode());
      return;
    }

    Timber.d("Query inventory was successful.");

    // Update the UI and purchases inventory with new list of purchases
    inventory.clear();
    add(result.getPurchasesList());
  }

  @Override
  public void onPurchasesUpdated(
      @BillingResponse int resultCode, List<com.android.billingclient.api.Purchase> purchases) {
    if (resultCode == BillingResponse.OK) {
      add(purchases);
    }
    String skus =
        purchases == null
            ? "null"
            : Joiner.on(";")
                .join(
                    Iterables.transform(purchases, com.android.billingclient.api.Purchase::getSku));
    Timber.i("onPurchasesUpdated(%s, %s)", BillingResponseToString(resultCode), skus);
    tracker.reportIabResult(resultCode, skus);
  }

  private void add(List<com.android.billingclient.api.Purchase> purchases) {
    inventory.add(Iterables.transform(purchases, Purchase::new));
  }

  @Override
  public void initiatePurchaseFlow(Activity activity, String skuId, String billingType) {
    executeServiceRequest(
        () -> {
          billingClient.launchBillingFlow(
              activity,
              BillingFlowParams.newBuilder()
                  .setSku(skuId)
                  .setType(billingType)
                  .setOldSkus(null)
                  .build());
        });
  }

  public void destroy() {
    Timber.d("Destroying the manager.");

    if (billingClient != null && billingClient.isReady()) {
      billingClient.endConnection();
      billingClient = null;
    }
  }

  private void startServiceConnection(final Runnable executeOnSuccess) {
    billingClient.startConnection(
        new com.android.billingclient.api.BillingClientStateListener() {
          @Override
          public void onBillingSetupFinished(@BillingResponse int billingResponseCode) {
            Timber.d("onBillingSetupFinished(%s)", billingResponseCode);

            if (billingResponseCode == BillingResponse.OK) {
              connected = true;
              if (executeOnSuccess != null) {
                executeOnSuccess.run();
              }
            }
            billingClientResponseCode = billingResponseCode;
          }

          @Override
          public void onBillingServiceDisconnected() {
            Timber.d("onBillingServiceDisconnected()");
            connected = false;
          }
        });
  }

  private void executeServiceRequest(Runnable runnable) {
    if (connected) {
      runnable.run();
    } else {
      // If billing service was disconnected, we try to reconnect 1 time.
      // (feel free to introduce your retry policy here).
      startServiceConnection(runnable);
    }
  }

  /**
   * Checks if subscriptions are supported for current client
   *
   * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED. It is only
   * used in unit tests and after queryPurchases execution, which already has a retry-mechanism
   * implemented.
   */
  private boolean areSubscriptionsSupported() {
    int responseCode = billingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
    if (responseCode != BillingResponse.OK) {
      Timber.d("areSubscriptionsSupported() got an error response: %s", responseCode);
    }
    return responseCode == BillingResponse.OK;
  }

  @Override
  public void observeSkuDetails(
      LifecycleOwner owner,
      Observer<List<SkuDetails>> subscriptionObserver,
      Observer<List<SkuDetails>> iapObserver) {
    subscriptions.observe(owner, subscriptionObserver);
    iaps.observe(owner, iapObserver);
  }

  @Override
  public void querySkuDetails() {
    executeServiceRequest(this::fetchSubscription);
  }

  private void fetchSubscription() {
    billingClient.querySkuDetailsAsync(
        SkuDetailsParams.newBuilder().setSkusList(SkuDetails.SKU_SUBS).setType(SkuType.SUBS).build(),
        new com.android.billingclient.api.SkuDetailsResponseListener() {
          @Override
          public void onSkuDetailsResponse(
              int responseCode, List<com.android.billingclient.api.SkuDetails> skuDetailsList) {
            if (responseCode == BillingResponse.OK) {
              subscriptions.setValue(transform(skuDetailsList, SkuDetails::new));
            } else {
              Timber.e(
                  "Query for subs failed: %s (%s)",
                  BillingResponseToString(responseCode), responseCode);
            }

            executeServiceRequest(BillingClientImpl.this::fetchIAPs);
          }
        });
  }

  private void fetchIAPs() {
    Iterable<String> purchased =
        transform(filter(inventory.getPurchases(), Purchase::isIap), Purchase::getSku);
    billingClient.querySkuDetailsAsync(
        SkuDetailsParams.newBuilder()
            .setSkusList(BuildConfig.DEBUG ? DEBUG_SKUS : newArrayList(purchased))
            .setType(SkuType.INAPP)
            .build(),
        new com.android.billingclient.api.SkuDetailsResponseListener() {
          @Override
          public void onSkuDetailsResponse(
              int responseCode, List<com.android.billingclient.api.SkuDetails> skuDetailsList) {
            if (responseCode == BillingResponse.OK) {
              iaps.setValue(transform(skuDetailsList, SkuDetails::new));
            } else {
              Timber.e(
                  "Query for iaps failed: %s (%s)",
                  BillingResponseToString(responseCode), responseCode);
            }
          }
        });
  }

  @Override
  public void consume(String sku) {
    if (!BuildConfig.DEBUG) {
      throw new IllegalStateException();
    }
    if (!inventory.purchased(sku)) {
      throw new IllegalArgumentException();
    }
    final ConsumeResponseListener onConsumeListener =
        (responseCode, purchaseToken1) -> {
          Timber.d("onConsumeResponse(%s, %s)", responseCode, purchaseToken1);
          queryPurchases();
        };

    executeServiceRequest(
        () ->
            billingClient.consumeAsync(
                inventory.getPurchase(sku).getPurchaseToken(), onConsumeListener));
  }

  @Override
  public int getErrorMessage() {
    return billingClientResponseCode == BillingResponse.BILLING_UNAVAILABLE
        ? R.string.error_billing_unavailable
        : R.string.error_billing_default;
  }
}
