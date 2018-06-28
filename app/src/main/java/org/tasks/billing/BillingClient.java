package org.tasks.billing;

import static com.google.common.collect.Iterables.transform;

import android.app.Activity;
import android.content.Context;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsParams.Builder;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.common.base.Joiner;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class BillingClient implements PurchasesUpdatedListener {

  private final Inventory inventory;
  private final LocalBroadcastManager localBroadcastManager;
  private final Tracker tracker;

  private com.android.billingclient.api.BillingClient billingClient;
  private boolean connected;
  private int billingClientResponseCode = -1;

  @Inject
  public BillingClient(
      @ForApplication Context context,
      Inventory inventory,
      LocalBroadcastManager localBroadcastManager,
      Tracker tracker) {
    this.inventory = inventory;
    this.localBroadcastManager = localBroadcastManager;
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

  public void initialize() {
    startServiceConnection(this::queryPurchases);
  }

  /**
   * Query purchases across various use cases and deliver the result in a formalized way through a
   * listener
   */
  public void queryPurchases() {
    Runnable queryToExecute =
        () -> {
          long time = System.currentTimeMillis();
          PurchasesResult purchasesResult = billingClient.queryPurchases(SkuType.INAPP);
          Timber.i("Querying purchases elapsed time: %sms", System.currentTimeMillis() - time);
          // If there are subscriptions supported, we add subscription rows as well
          if (areSubscriptionsSupported()) {
            PurchasesResult subscriptionResult = billingClient.queryPurchases(SkuType.SUBS);
            Timber.i(
                "Querying purchases and subscriptions elapsed time: %sms",
                System.currentTimeMillis() - time);
            Timber.i(
                "Querying subscriptions result code: %s res: %s",
                subscriptionResult.getResponseCode(), subscriptionResult.getPurchasesList());
            if (subscriptionResult.getResponseCode() == BillingResponse.OK) {
              purchasesResult.getPurchasesList().addAll(subscriptionResult.getPurchasesList());
            } else {
              Timber.e("Got an error response trying to query subscription purchases");
            }
          } else if (purchasesResult.getResponseCode() == BillingResponse.OK) {
            Timber.i("Skipped subscription purchases query since they are not supported");
          } else {
            Timber.w(
                "queryPurchases() got an error response code: %s",
                purchasesResult.getResponseCode());
          }
          onQueryPurchasesFinished(purchasesResult);
        };

    executeServiceRequest(queryToExecute);
  }

  /** Handle a result from querying of purchases and report an updated list to the listener */
  private void onQueryPurchasesFinished(PurchasesResult result) {
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
  public void onPurchasesUpdated(@BillingResponse int resultCode, List<Purchase> purchases) {
    if (resultCode == BillingResponse.OK) {
      add(purchases);
    }
    String skus =
        purchases == null ? "null" : Joiner.on(";").join(transform(purchases, Purchase::getSku));
    Timber.i("onPurchasesUpdated(%s, %s)", BillingResponseToString(resultCode), skus);
    tracker.reportIabResult(resultCode, skus);
  }

  private void add(List<Purchase> purchases) {
    inventory.add(purchases);
    localBroadcastManager.broadcastPurchasesUpdated();
  }

  /** Start a purchase flow */
  void initiatePurchaseFlow(
      Activity activity, final String skuId, final @SkuType String billingType) {
    Runnable purchaseFlowRequest =
        () -> {
          Timber.d("Launching in-app purchase flow");
          BillingFlowParams purchaseParams =
              BillingFlowParams.newBuilder()
                  .setSku(skuId)
                  .setType(billingType)
                  .setOldSkus(null)
                  .build();
          billingClient.launchBillingFlow(activity, purchaseParams);
        };

    executeServiceRequest(purchaseFlowRequest);
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

  public void querySkuDetailsAsync(
      @SkuType final String itemType,
      final List<String> skuList,
      final SkuDetailsResponseListener listener) {
    Runnable request =
        () -> {
          Builder params = SkuDetailsParams.newBuilder();
          params.setSkusList(skuList).setType(itemType);
          billingClient.querySkuDetailsAsync(params.build(), listener);
        };
    executeServiceRequest(request);
  }

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

    Runnable request =
        () ->
            billingClient.consumeAsync(
                inventory.getPurchase(sku).getPurchaseToken(), onConsumeListener);
    executeServiceRequest(request);
  }

  public int getBillingClientResponseCode() {
    return billingClientResponseCode;
  }
}
