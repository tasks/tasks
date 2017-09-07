package org.tasks.billing;

import android.app.Activity;
import android.content.Intent;

import javax.inject.Inject;

public class PurchaseHelper {

    @Inject
    public PurchaseHelper() {
    }

    public boolean purchase(final Activity activity,
                            final String sku, final String pref,
                            final int requestCode, final PurchaseHelperCallback callback) {
        callback.purchaseCompleted(false, sku);
        return false;
    }

    public void handleActivityResult(PurchaseHelperCallback callback, int requestCode, int resultCode, Intent data) {

    }

    public void disposeIabHelper() {

    }

    public void consumePurchases() {

    }
}
