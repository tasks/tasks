package org.tasks.billing;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.android.vending.billing.IabBroadcastReceiver;
import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;
import com.google.common.base.Strings;

import org.tasks.Broadcaster;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.isAppInstalled;

@Singleton
public class PurchaseHelper implements IabHelper.OnIabSetupFinishedListener, IabHelper.QueryInventoryFinishedListener, IabBroadcastReceiver.IabBroadcastListener {

    private final IabHelper iabHelper;
    private final Context context;
    private final Preferences preferences;
    private final Tracker tracker;
    private final Broadcaster broadcaster;

    private Inventory inventory;
    private PurchaseHelperCallback activityResultCallback;

    @Inject
    public PurchaseHelper(@ForApplication Context context, Preferences preferences, Tracker tracker,
                          Broadcaster broadcaster) {
        this.context = context;
        this.preferences = preferences;
        this.tracker = tracker;
        this.broadcaster = broadcaster;
        iabHelper = new IabHelper(context, context.getString(R.string.gp_key));
    }

    public void initialize() {
        iabHelper.startSetup(this);
        context.registerReceiver(new IabBroadcastReceiver(this), new IntentFilter(IabBroadcastReceiver.ACTION));
    }

    @Override
    public void onIabSetupFinished(IabResult result) {
        if (result.isSuccess()) {
            iabHelper.queryInventoryAsync(this);
        } else {
            Timber.e("in-app billing setup failed: %s", result.getMessage());
        }
    }

    @Override
    public void onQueryInventoryFinished(final IabResult result, Inventory inv) {
        if (result.isSuccess()) {
            inventory = inv;
            checkPurchase(R.string.sku_tasker, R.string.p_purchased_tasker);
            checkPurchase(R.string.sku_tesla_unread, R.string.p_purchased_tesla_unread);
            checkPurchase(R.string.sku_dashclock, R.string.p_purchased_dashclock);
        } else {
            Timber.e("in-app billing inventory query failed: %s", result.getMessage());
        }
    }

    private void checkPurchase(int skuRes, final int prefRes) {
        final String sku = context.getString(skuRes);
        if (inventory.hasPurchase(sku)) {
            Timber.d("Found purchase: %s", sku);
            preferences.setBoolean(prefRes, true);
        } else {
            Timber.d("No purchase: %s", sku);
        }
    }

    @Override
    public void receivedBroadcast() {
        try {
            iabHelper.queryInventoryAsync(this);
        } catch(IllegalStateException e) {
            tracker.reportException(e);
        }
    }

    public void purchase(DialogBuilder dialogBuilder, final Activity activity, final String sku, final String pref, final int requestCode, final PurchaseHelperCallback callback) {
        if (activity.getString(R.string.sku_tasker).equals(sku) && isAppInstalled(activity, "org.tasks.locale")) {
            dialogBuilder.newMessageDialog(R.string.tasker_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.buy, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            launchPurchaseFlow(activity, sku, pref, requestCode, callback);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            callback.purchaseCompleted(false, sku);
                        }
                    })
                    .show();
        } else {
            launchPurchaseFlow(activity, sku, pref, requestCode, callback);
        }
    }

    public void consumePurchases() {
        if (BuildConfig.DEBUG) {
            List<Purchase> purchases = new ArrayList<>();
            final Purchase tasker = inventory.getPurchase(context.getString(R.string.sku_tasker));
            final Purchase dashclock = inventory.getPurchase(context.getString(R.string.sku_dashclock));
            final Purchase teslaUnread = inventory.getPurchase(context.getString(R.string.sku_tesla_unread));
            if (tasker != null) {
                purchases.add(tasker);
            }
            if (dashclock != null) {
                purchases.add(dashclock);
            }
            if (teslaUnread != null) {
                purchases.add(teslaUnread);
            }
            iabHelper.consumeAsync(purchases, new IabHelper.OnConsumeMultiFinishedListener() {
                @Override
                public void onConsumeMultiFinished(List<Purchase> purchases, List<IabResult> results) {
                    for (int i = 0 ; i < purchases.size() ; i++) {
                        Purchase purchase = purchases.get(i);
                        IabResult iabResult = results.get(i);
                        if (iabResult.isSuccess()) {
                            if (purchase.equals(tasker)) {
                                preferences.setBoolean(R.string.p_purchased_tasker, false);
                            } else if (purchase.equals(dashclock)) {
                                preferences.setBoolean(R.string.p_purchased_dashclock, false);
                            } else if (purchase.equals(teslaUnread)) {
                                preferences.setBoolean(R.string.p_purchased_tesla_unread, false);
                                preferences.setBoolean(R.string.p_tesla_unread_enabled, false);
                            } else {
                                Timber.e("Unhandled consumption for purchase: %s", purchase);
                            }
                            inventory.erasePurchase(purchase.getSku());
                            Timber.d("Consumed %s", purchase);
                        } else {
                            Timber.e("Consume failed: %s, %s", purchase, iabResult);
                        }
                    }
                }
            });
        }
    }

    private void launchPurchaseFlow(final Activity activity, final String sku, final String pref, int requestCode, PurchaseHelperCallback callback) {
        try {
            iabHelper.launchPurchaseFlow(activity, sku, requestCode, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    Timber.d(result.toString());
                    tracker.reportIabResult(result, info);
                    if (result.isSuccess()) {
                        if (!Strings.isNullOrEmpty(pref)) {
                            preferences.setBoolean(pref, true);
                            broadcaster.refresh();
                        }
                    } else if (result.getResponse() != IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED &&
                            result.getResponse() != IabHelper.IABHELPER_USER_CANCELLED) {
                        Toast.makeText(activity, result.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    activityResultCallback.purchaseCompleted(result.isSuccess(), sku);
                }
            });
        } catch (IllegalStateException e) {
            tracker.reportException(e);
            Toast.makeText(activity, R.string.billing_service_busy, Toast.LENGTH_LONG).show();
            callback.purchaseCompleted(false, sku);
        }
    }

    public void handleActivityResult(PurchaseHelperCallback callback, int requestCode, int resultCode, Intent data) {
        this.activityResultCallback = callback;

        iabHelper.handleActivityResult(requestCode, resultCode, data);
    }
}
