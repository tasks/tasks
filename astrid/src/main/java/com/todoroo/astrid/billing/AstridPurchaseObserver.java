package com.todoroo.astrid.billing;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.billing.BillingConstants.PurchaseState;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;
import com.todoroo.astrid.billing.BillingService.RequestPurchase;
import com.todoroo.astrid.billing.BillingService.RestoreTransactions;

import static com.todoroo.andlib.utility.Preferences.getBoolean;
import static com.todoroo.andlib.utility.Preferences.getInt;
import static com.todoroo.andlib.utility.Preferences.getStringValue;

public class AstridPurchaseObserver extends PurchaseObserver {
    private static final String PREF_PRODUCT_ID = ActFmPreferenceService.IDENTIFIER + "_inapp_product_id";
    private static final String PREF_PURCHASE_STATE = ActFmPreferenceService.IDENTIFIER + "_inapp_purchase_state";
    private static final String PREF_TRANSACTIONS_INITIALIZED = "premium_transactions_initialized"; //$NON-NLS-1$

    private boolean billingSupported;
    private BillingService billingService;

    public AstridPurchaseObserver(Activity activity, BillingService billingService) {
        super(activity, new Handler());
        this.billingService = billingService;
    }

    public boolean isBillingSupported() {
        return billingSupported;
    }

    public boolean userDonated() {
        return BillingConstants.TASKS_DONATION_ITEM_ID.equals(getStringValue(PREF_PRODUCT_ID)) &&
                getInt(PREF_PURCHASE_STATE, -1) == PurchaseState.PURCHASED.ordinal();
    }

    @Override
    public void onBillingSupported(boolean supported, String type) {
        Log.d(TAG, "onBillingSupported(" + supported + ", " + type + ")");
        if (BillingConstants.ITEM_TYPE_INAPP.equals(type)) {
            billingSupported = supported;
            if (supported && !getBoolean(PREF_TRANSACTIONS_INITIALIZED, false)) {
                billingService.restoreTransactions();
            }
        }
    }

    @Override
    public void onPurchaseStateChange(PurchaseState purchaseState, final String itemId) {
        Log.d(TAG, "onPurchaseStateChange(" + purchaseState + ", " + itemId + ")");
        if (BillingConstants.TASKS_DONATION_ITEM_ID.equals(itemId)) {
            Preferences.setString(PREF_PRODUCT_ID, itemId);
            Preferences.setInt(PREF_PURCHASE_STATE, purchaseState.ordinal());
        }
    }

    @Override
    public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
        Log.d(TAG, "onRequestPurchaseResponse(" + request + ", " + responseCode + ")");
    }

    @Override
    public void onRestoreTransactionsResponse(RestoreTransactions request, ResponseCode responseCode) {
        Log.d(TAG, "onRestoreTransactionsResponse(" + request + ", " + responseCode + ")");
        if (responseCode == ResponseCode.RESULT_OK) {
            Preferences.setBoolean(PREF_TRANSACTIONS_INITIALIZED, true);
        }
    }
}
