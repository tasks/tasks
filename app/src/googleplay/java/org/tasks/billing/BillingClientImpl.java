package org.tasks.billing;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingFlowParams.ProrationMode;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import org.tasks.BuildConfig;
import org.tasks.analytics.Firebase;
import org.tasks.injection.ApplicationContext;
import timber.log.Timber;

@SuppressWarnings("all")
public class BillingClientImpl implements BillingClient, PurchasesUpdatedListener {

  public static final String TYPE_SUBS = SkuType.SUBS;

  private final Inventory inventory;
  private final Firebase firebase;
  private com.android.billingclient.api.BillingClient billingClient;
  private boolean connected;
  private OnPurchasesUpdated onPurchasesUpdated;

  public BillingClientImpl(@ApplicationContext Context context, Inventory inventory, Firebase firebase) {
    this.inventory = inventory;
    this.firebase = firebase;
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
    boolean success = resultCode == BillingResponse.OK;
    if (success) {
      add(purchases);
    }
    if (onPurchasesUpdated != null) {
      onPurchasesUpdated.onPurchasesUpdated(success);
    }
    String skus =
        purchases == null
            ? "null"
            : Joiner.on(";")
                .join(
                    Iterables.transform(purchases, com.android.billingclient.api.Purchase::getSku));
    Timber.i("onPurchasesUpdated(%s, %s)", BillingResponseToString(resultCode), skus);
    firebase.reportIabResult(resultCode, skus);
  }

  private void add(List<com.android.billingclient.api.Purchase> purchases) {
    inventory.add(Iterables.transform(purchases, Purchase::new));
  }

  @Override
  public void initiatePurchaseFlow(
      Activity activity, String skuId, String billingType, @Nullable String oldSku) {
    executeServiceRequest(
        () ->
            billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setSku(skuId)
                    .setType(billingType)
                    .setOldSkus(oldSku == null ? null : newArrayList(oldSku))
                    .setReplaceSkusProrationMode(ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
                    .build()));
  }

  @Override
  public void addPurchaseCallback(OnPurchasesUpdated onPurchasesUpdated) {
    this.onPurchasesUpdated = onPurchasesUpdated;
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
}
