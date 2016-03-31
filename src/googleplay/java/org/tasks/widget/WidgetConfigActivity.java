package org.tasks.widget;

import android.content.Intent;

import org.tasks.R;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.injection.ActivityComponent;

import javax.inject.Inject;

import timber.log.Timber;

public class WidgetConfigActivity extends BaseWidgetConfigActivity implements PurchaseHelperCallback {

    private static final int REQUEST_PURCHASE = 10109;

    @Inject PurchaseHelper purchaseHelper;

    @Override
    public void initiateThemePurchase() {
        purchaseHelper.purchase(dialogBuilder, this, getString(R.string.sku_themes), getString(R.string.p_purchased_themes), REQUEST_PURCHASE, this);
    }

    @Override
    public void purchaseCompleted(boolean success, final String sku) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getString(R.string.sku_themes).equals(sku)) {
                    showThemeSelection();
                } else {
                    Timber.d("Unhandled sku: %s", sku);
                }
            }
        });
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE) {
            purchaseHelper.handleActivityResult(this, requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
