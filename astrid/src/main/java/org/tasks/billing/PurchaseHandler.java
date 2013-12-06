package org.tasks.billing;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.billing.BillingConstants;
import com.todoroo.astrid.billing.BillingConstants.PurchaseState;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;
import com.todoroo.astrid.billing.BillingService;

import static com.todoroo.andlib.utility.Preferences.getBoolean;
import static com.todoroo.andlib.utility.Preferences.getInt;
import static com.todoroo.andlib.utility.Preferences.getStringValue;

public class PurchaseHandler {

    private static final String PREF_PRODUCT_ID = ActFmPreferenceService.IDENTIFIER + "_inapp_product_id";
    private static final String PREF_PURCHASE_STATE = ActFmPreferenceService.IDENTIFIER + "_inapp_purchase_state";
    private static final String PREF_TRANSACTIONS_INITIALIZED = "premium_transactions_initialized"; //$NON-NLS-1$

    private boolean billingSupported;
    private boolean userDonated;
    private BillingService billingService;

    public PurchaseHandler(BillingService billingService) {
        this.billingService = billingService;
        updateDonationStatus();
    }

    public boolean isBillingSupported() {
        return billingSupported;
    }

    public boolean userDonated() {
        return userDonated;
    }

    private void updateDonationStatus() {
        userDonated = BillingConstants.TASKS_DONATION_ITEM_ID.equals(getStringValue(PREF_PRODUCT_ID)) &&
                getInt(PREF_PURCHASE_STATE, -1) == PurchaseState.PURCHASED.ordinal();
    }

    public void onBillingSupported(boolean supported, String type) {
        if (BillingConstants.ITEM_TYPE_INAPP.equals(type)) {
            billingSupported = supported;
            if (supported && !restoredTransactions()) {
                billingService.restoreTransactions();
            }
        }
    }

    public void onPurchaseStateChange(PurchaseState purchaseState, final String itemId) {
        if (BillingConstants.TASKS_DONATION_ITEM_ID.equals(itemId)) {
            Preferences.setString(PREF_PRODUCT_ID, itemId);
            Preferences.setInt(PREF_PURCHASE_STATE, purchaseState.ordinal());
            updateDonationStatus();
        }
    }

    public void onRestoreTransactionsResponse(ResponseCode responseCode) {
        if (responseCode == ResponseCode.RESULT_OK) {
            Preferences.setBoolean(PREF_TRANSACTIONS_INITIALIZED, true);
        }
    }

    boolean restoredTransactions() {
        return getBoolean(PREF_TRANSACTIONS_INITIALIZED, false);
    }
}
