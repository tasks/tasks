package org.tasks.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Purchase;
import com.google.common.base.Strings;

import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

@ApplicationScope
public class PurchaseHelper implements IabHelper.OnIabSetupFinishedListener {

    private final Context context;
    private final Preferences preferences;
    private final Tracker tracker;
    private final InventoryHelper inventory;
    private final Executor executor;
    private LocalBroadcastManager localBroadcastManager;

    private PurchaseHelperCallback activityResultCallback;
    private IabHelper iabHelper;

    @Inject
    public PurchaseHelper(@ForApplication Context context, Preferences preferences, Tracker tracker,
                          InventoryHelper inventory, @Named("iab-executor") Executor executor,
                          LocalBroadcastManager localBroadcastManager) {
        this.context = context;
        this.preferences = preferences;
        this.tracker = tracker;
        this.inventory = inventory;
        this.executor = executor;
        this.localBroadcastManager = localBroadcastManager;
    }

    @Override
    public void onIabSetupFinished(IabResult result) {
        if (result.isFailure()) {
            Timber.e("in-app billing setup failed: %s", result.getMessage());
        }
    }

    public boolean purchase(final Activity activity, final String sku, final String pref, final int requestCode, final PurchaseHelperCallback callback) {
        launchPurchaseFlow(activity, sku, pref, requestCode, callback);
        return true;
    }

    public void consumePurchases() {
        if (BuildConfig.DEBUG) {
            final List<Purchase> purchases = new ArrayList<>();
            final Purchase tasker = inventory.getPurchase(context.getString(R.string.sku_tasker));
            final Purchase dashclock = inventory.getPurchase(context.getString(R.string.sku_dashclock));
            final Purchase themes = inventory.getPurchase(context.getString(R.string.sku_themes));
            if (tasker != null) {
                purchases.add(tasker);
            }
            if (dashclock != null) {
                purchases.add(dashclock);
            }
            if (themes != null) {
                purchases.add(themes);
            }
            final IabHelper iabHelper = new IabHelper(context, context.getString(R.string.gp_key), executor);
            iabHelper.enableDebugLogging(true);
            iabHelper.startSetup(result -> {
                if (result.isSuccess()) {
                    iabHelper.consumeAsync(purchases, (purchases1, results) -> {
                        for (int i = 0; i < purchases1.size() ; i++) {
                            Purchase purchase = purchases1.get(i);
                            IabResult iabResult = results.get(i);
                            if (iabResult.isSuccess()) {
                                if (purchase.equals(tasker)) {
                                    preferences.setBoolean(R.string.p_purchased_tasker, false);
                                } else if (purchase.equals(dashclock)) {
                                    preferences.setBoolean(R.string.p_purchased_dashclock, false);
                                } else if (purchase.equals(themes)) {
                                    preferences.setBoolean(R.string.p_purchased_themes, false);
                                } else {
                                    Timber.e("Unhandled consumption for purchase: %s", purchase);
                                }
                                inventory.erasePurchase(purchase.getSku());
                                Timber.d("Consumed %s", purchase);
                            } else {
                                Timber.e("Consume failed: %s, %s", purchase, iabResult);
                            }
                        }
                        iabHelper.dispose();
                    });
                } else {
                    Timber.e("setup failed: %s", result.getMessage());
                    iabHelper.dispose();
                }
            });
        }
    }

    private void launchPurchaseFlow(final Activity activity, final String sku, final String pref, final int requestCode, final PurchaseHelperCallback callback) {
        if (iabHelper != null) {
            Toast.makeText(activity, R.string.billing_service_busy, Toast.LENGTH_LONG).show();
            callback.purchaseCompleted(false, sku);
            return;
        }
        iabHelper = new IabHelper(context, context.getString(R.string.gp_key), executor);
        iabHelper.enableDebugLogging(BuildConfig.DEBUG);
        Timber.d("%s: startSetup", iabHelper);
        iabHelper.startSetup(result -> {
            if (result.isSuccess()) {
                try {
                    Timber.d("%s: launchPurchaseFlow for %s", iabHelper, sku);
                    iabHelper.launchPurchaseFlow(activity, sku, requestCode, (result1, info) -> {
                        Timber.d(result1.toString());
                        tracker.reportIabResult(result1, sku);
                        if (result1.isSuccess()) {
                            if (!Strings.isNullOrEmpty(pref)) {
                                preferences.setBoolean(pref, true);
                                localBroadcastManager.broadcastRefresh();
                            }
                            inventory.refreshInventory();
                        } else if (result1.getResponse() != IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED &&
                                result1.getResponse() != IabHelper.IABHELPER_USER_CANCELLED) {
                            Toast.makeText(activity, result1.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        if (activityResultCallback != null) {
                            activityResultCallback.purchaseCompleted(result1.isSuccess(), sku);
                        }
                        disposeIabHelper();
                    });
                } catch (IllegalStateException e) {
                    tracker.reportException(e);
                    Toast.makeText(activity, R.string.billing_service_busy, Toast.LENGTH_LONG).show();
                    callback.purchaseCompleted(false, sku);
                    disposeIabHelper();
                }
            } else {
                Timber.e(result.toString());
                Toast.makeText(activity, result.getMessage(), Toast.LENGTH_LONG).show();
                callback.purchaseCompleted(false, sku);
                disposeIabHelper();
            }
        });
    }

    public void disposeIabHelper() {
        if (iabHelper != null) {
            Timber.d("%s: dispose", iabHelper);
            iabHelper.dispose();
            iabHelper = null;
        }
    }

    public void handleActivityResult(PurchaseHelperCallback callback, int requestCode, int resultCode, Intent data) {
        this.activityResultCallback = callback;

        if (iabHelper != null) {
            iabHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }
}
