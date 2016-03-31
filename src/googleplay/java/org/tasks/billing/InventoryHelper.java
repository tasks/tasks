package org.tasks.billing;

import android.content.Context;
import android.content.IntentFilter;

import com.android.vending.billing.IabBroadcastReceiver;
import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class InventoryHelper implements IabBroadcastReceiver.IabBroadcastListener {

    private final Context context;
    private final Preferences preferences;
    private final Broadcaster broadcaster;

    private Inventory inventory;

    @Inject
    public InventoryHelper(@ForApplication Context context, Preferences preferences, Broadcaster broadcaster) {
        this.context = context;
        this.preferences = preferences;
        this.broadcaster = broadcaster;
    }

    public void initialize() {
        context.registerReceiver(new IabBroadcastReceiver(this), new IntentFilter(IabBroadcastReceiver.ACTION));
        refreshInventory();
    }

    public void refreshInventory() {
        final IabHelper helper = new IabHelper(context, context.getString(R.string.gp_key));
        helper.startSetup(getSetupListener(helper));
    }

    private IabHelper.OnIabSetupFinishedListener getSetupListener(final IabHelper helper) {
        return new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    helper.queryInventoryAsync(getQueryListener(helper));
                } else {
                    Timber.e("setup failed: %s", result.getMessage());
                    helper.dispose();
                }
            }
        };
    }

    private IabHelper.QueryInventoryFinishedListener getQueryListener(final IabHelper helper) {
        return new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                if (result.isSuccess()) {
                    inventory = inv;
                    checkPurchase(R.string.sku_tasker, R.string.p_purchased_tasker);
                    checkPurchase(R.string.sku_tesla_unread, R.string.p_purchased_tesla_unread);
                    checkPurchase(R.string.sku_dashclock, R.string.p_purchased_dashclock);
                    checkPurchase(R.string.sku_themes, R.string.p_purchased_themes);
                    broadcaster.refresh();
                } else {
                    Timber.e("query inventory failed: %s", result.getMessage());
                }
                helper.dispose();
            }
        };
    }

    @Override
    public void receivedBroadcast() {
        refreshInventory();
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

    public void erasePurchase(String sku) {
        inventory.erasePurchase(sku);
    }

    public Purchase getPurchase(String sku) {
        return inventory.getPurchase(sku);
    }
}
