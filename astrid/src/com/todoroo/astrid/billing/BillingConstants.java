package com.todoroo.astrid.billing;

import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.utility.Constants;


@SuppressWarnings("nls")
public class BillingConstants {

    /** This is the action we use to bind to the MarketBillingService. */
    public static final String MARKET_BILLING_SERVICE_ACTION = "com.android.vending.billing.MarketBillingService.BIND";

    // Intent actions that we send from the BillingReceiver to the
    // BillingService.  Defined by this application.
    public static final String ACTION_CONFIRM_NOTIFICATION = "com.timsu.astrid.subscriptions.CONFIRM_NOTIFICATION";
    public static final String ACTION_GET_PURCHASE_INFORMATION = "com.timsu.astrid.subscriptions.GET_PURCHASE_INFORMATION";
    public static final String ACTION_RESTORE_TRANSACTIONS = "com.timsu.astrid.subscriptions.RESTORE_TRANSACTIONS";

    // Intent actions that we receive in the BillingReceiver from Market.
    // These are defined by Market and cannot be changed.
    public static final String ACTION_NOTIFY = "com.android.vending.billing.IN_APP_NOTIFY";
    public static final String ACTION_RESPONSE_CODE = "com.android.vending.billing.RESPONSE_CODE";
    public static final String ACTION_PURCHASE_STATE_CHANGED = "com.android.vending.billing.PURCHASE_STATE_CHANGED";

    // These are the names of the extras that are passed in an intent from
    // Market to this application and cannot be changed.
    public static final String NOTIFICATION_ID = "notification_id";
    public static final String INAPP_SIGNED_DATA = "inapp_signed_data";
    public static final String INAPP_SIGNATURE = "inapp_signature";
    public static final String INAPP_REQUEST_ID = "request_id";
    public static final String INAPP_RESPONSE_CODE = "response_code";

    // These are the names of the fields in the request bundle.
    public static final String BILLING_REQUEST_METHOD = "BILLING_REQUEST";
    public static final String BILLING_REQUEST_API_VERSION = "API_VERSION";
    public static final String BILLING_REQUEST_PACKAGE_NAME = "PACKAGE_NAME";
    public static final String BILLING_REQUEST_ITEM_ID = "ITEM_ID";
    public static final String BILLING_REQUEST_ITEM_TYPE = "ITEM_TYPE";
    public static final String BILLING_REQUEST_DEVELOPER_PAYLOAD = "DEVELOPER_PAYLOAD";
    public static final String BILLING_REQUEST_NOTIFY_IDS = "NOTIFY_IDS";
    public static final String BILLING_REQUEST_NONCE = "NONCE";

    public static final String BILLING_RESPONSE_RESPONSE_CODE = "RESPONSE_CODE";
    public static final String BILLING_RESPONSE_PURCHASE_INTENT = "PURCHASE_INTENT";
    public static final String BILLING_RESPONSE_REQUEST_ID = "REQUEST_ID";
    public static final long BILLING_RESPONSE_INVALID_REQUEST_ID = -1;

    // These are the types supported in the IAB v2
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBSCRIPTION = "subs";


    public static final String PRODUCT_ID_MONTHLY = "com.timsu.astrid.premium_monthly";
    public static final String PRODUCT_ID_YEARLY = "com.timsu.astrid.premium_yearly";

    public static final String PREF_PRODUCT_ID = ActFmPreferenceService.IDENTIFIER + "_inapp_product_id";
    public static final String PREF_PURCHASE_TOKEN = ActFmPreferenceService.IDENTIFIER + "_inapp_purchase_token";
    public static final String PREF_NEEDS_SERVER_UPDATE = ActFmPreferenceService.IDENTIFIER + "_inapp_needs_server_update";
    public static final String PREF_TRANSACTIONS_INITIALIZED = "premium_transactions_initialized"; //$NON-NLS-1$

    public static final char PUB_KEY_OBFUSCATION_CHAR = '!';
    public static final char PUB_KEY_REPLACE_CHAR = 'B';
    public static final String PUB_KEY_OBFUSCATED = "pubkey";

    public static final boolean DEBUG = false || Constants.DEBUG;

    // The response codes for a request, defined by Android Market.
    public enum ResponseCode {
        RESULT_OK,
        RESULT_USER_CANCELED,
        RESULT_SERVICE_UNAVAILABLE,
        RESULT_BILLING_UNAVAILABLE,
        RESULT_ITEM_UNAVAILABLE,
        RESULT_DEVELOPER_ERROR,
        RESULT_ERROR;

        // Converts from an ordinal value to the ResponseCode
        public static ResponseCode valueOf(int index) {
            ResponseCode[] values = ResponseCode.values();
            if (index < 0 || index >= values.length) {
                return RESULT_ERROR;
            }
            return values[index];
        }
    }

    // The possible states of an in-app purchase, as defined by Android Market.
    public enum PurchaseState {
        // Responses to requestPurchase or restoreTransactions.
        PURCHASED,   // User was charged for the order.
        CANCELED,    // The charge failed on the server. (NOT THE SAME AS CANCELING A SUBSCRIPTION)
        REFUNDED,    // User received a refund for the order.
        EXPIRED;     // Subscription expired due to non-payment or cancellation

        // Converts from an ordinal value to the PurchaseState
        public static PurchaseState valueOf(int index) {
            PurchaseState[] values = PurchaseState.values();
            if (index < 0 || index >= values.length) {
                return CANCELED;
            }
            return values[index];
        }
    }
}
