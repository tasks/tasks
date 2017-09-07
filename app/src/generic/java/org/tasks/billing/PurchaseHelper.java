package org.tasks.billing;

import android.app.Activity;
import android.content.Intent;

import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class PurchaseHelper {
    private Preferences preferences;

    @Inject
    public PurchaseHelper(Preferences preferences) {
        this.preferences = preferences;
    }

    public boolean purchase(final Activity activity,
                            final String sku, final String pref,
                            final int requestCode, final PurchaseHelperCallback callback) {
        preferences.setBoolean(pref, true);
        callback.purchaseCompleted(true, sku);
        return true;
    }

    public void handleActivityResult(PurchaseHelperCallback callback, int requestCode, int resultCode, Intent data) {

    }

    public void disposeIabHelper() {

    }

    public void consumePurchases() {

    }
}
