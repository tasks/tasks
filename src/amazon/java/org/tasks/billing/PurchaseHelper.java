package org.tasks.billing;

import android.app.Activity;
import android.content.Intent;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.widget.WidgetConfigActivity;

import javax.inject.Inject;

public class PurchaseHelper {

    @Inject
    public PurchaseHelper() {
    }

    public boolean purchase(DialogBuilder dialogBuilder, final Activity activity,
                            final String sku, final String pref,
                            final int requestCode, final PurchaseHelperCallback callback) {
        callback.purchaseCompleted(false, sku);
        return false;
    }

    public void handleActivityResult(WidgetConfigActivity widgetConfigActivity, int requestCode, int resultCode, Intent data) {

    }
}
