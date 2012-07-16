package com.todoroo.astrid.billing;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.billing.BillingConstants.PurchaseState;
import com.todoroo.astrid.billing.BillingConstants.ResponseCode;
import com.todoroo.astrid.billing.BillingService.RequestPurchase;
import com.todoroo.astrid.billing.BillingService.RestoreTransactions;
import com.todoroo.astrid.utility.Constants;

public class BillingActivity extends Activity {

    private static final int DIALOG_CANNOT_CONNECT_ID = 1;
    private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
    private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;

    private static final String TRANSACTIONS_INITIALIZED = "premium_transactions_initialized"; //$NON-NLS-1$

    private Handler handler;
    private BillingService billingService;
    private AstridPurchaseObserver purchaseObserver;
    private Button buyMonth;
    private Button buyYear;

    @Autowired private ActFmInvoker actFmInvoker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.billing_activity);

        setupButtons();

        handler = new Handler();
        billingService = new BillingService();
        billingService.setContext(this);
        purchaseObserver = new AstridPurchaseObserver(handler);

        ResponseHandler.register(purchaseObserver);

        if (!billingService.checkBillingSupported(BillingConstants.ITEM_TYPE_SUBSCRIPTION)) {
            showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
        } else if (ActFmPreferenceService.isPremiumUser()) {
            DialogUtilities.okDialog(this, getString(R.string.premium_already_subscribed), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
    }

    private void setupButtons() {
        buyMonth = (Button) findViewById(R.id.premium_buy_month);
        buyYear = (Button) findViewById(R.id.premium_buy_year);

        buyMonth.setEnabled(false);
        buyYear.setEnabled(false);

        buyMonth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!billingService.requestPurchase(BillingConstants.PRODUCT_ID_MONTHLY,
                        BillingConstants.ITEM_TYPE_SUBSCRIPTION, null)) {
                    showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
                }
            }
        });

        buyYear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!billingService.requestPurchase(BillingConstants.PRODUCT_ID_YEARLY,
                        BillingConstants.ITEM_TYPE_SUBSCRIPTION, null)) {
                    showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
                }
            }
        });
    }

    /**
     * Replaces the language and/or country of the device into the given string.
     * The pattern "%lang%" will be replaced by the device's language code and
     * the pattern "%region%" will be replaced with the device's country code.
     *
     * @param str the string to replace the language/country within
     * @return a string containing the local language and region codes
     */
    @SuppressWarnings("nls")
    private String replaceLanguageAndRegion(String str) {
        // Substitute language and or region if present in string
        if (str.contains("%lang%") || str.contains("%region%")) {
            Locale locale = Locale.getDefault();
            str = str.replace("%lang%", locale.getLanguage().toLowerCase());
            str = str.replace("%region%", locale.getCountry().toLowerCase());
        }
        return str;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CANNOT_CONNECT_ID:
            return createDialog(R.string.cannot_connect_title,
                    R.string.cannot_connect_message);
        case DIALOG_BILLING_NOT_SUPPORTED_ID:
            return createDialog(R.string.billing_not_supported_title,
                    R.string.billing_not_supported_message);
            case DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID:
                return createDialog(R.string.subscriptions_not_supported_title,
                        R.string.subscriptions_not_supported_message);
        default:
            return null;
        }
    }

    private Dialog createDialog(int titleId, int messageId) {
        String helpUrl = replaceLanguageAndRegion(getString(R.string.subscriptions_help_url));
        if (Constants.DEBUG) {
            Log.i("billing-activity-url", helpUrl); //$NON-NLS-1$
        }
        final Uri helpUri = Uri.parse(helpUrl);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId)
            .setIcon(android.R.drawable.stat_sys_warning)
            .setMessage(messageId)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.subscriptions_learn_more, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, helpUri);
                    startActivity(intent);
                }
            });
        return builder.create();
    }

    private void restoreTransactions() {
        boolean initialized = Preferences.getBoolean(TRANSACTIONS_INITIALIZED, false);
        if (!initialized) {
            billingService.restoreTransactions();
            Toast.makeText(this, R.string.restoring_transactions, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    @SuppressWarnings("nls")
    private class AstridPurchaseObserver extends PurchaseObserver {
        public AstridPurchaseObserver(Handler handler) {
            super(BillingActivity.this, handler);
        }

        @Override
        public void onBillingSupported(boolean supported, String type) {
            if (Constants.DEBUG) {
                Log.i(TAG, "supported: " + supported);
            }
            if (type != null && type.equals(BillingConstants.ITEM_TYPE_SUBSCRIPTION)) {
                if (supported) {
                    restoreTransactions();
                    buyMonth.setEnabled(true);
                    buyYear.setEnabled(true);
                } else {
                    showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
                }
            } else {
                showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
            }
        }

        @Override
        public void onPurchaseStateChange(PurchaseState purchaseState, String itemId,
                int quantity, long purchaseTime, String developerPayload, String purchaseToken) {
            if (Constants.DEBUG) {
                Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
            }

            Preferences.setString(BillingConstants.PREF_PRODUCT_ID, itemId);
            Preferences.setString(BillingConstants.PREF_PURCHASE_TOKEN, purchaseToken);

            if (purchaseState == PurchaseState.PURCHASED) {
                try {
                    actFmInvoker.invoke("premium_update_android", "purchaseToken", purchaseToken, "productId", itemId);
                    Preferences.setBoolean(ActFmPreferenceService.PREF_PREMIUM, true);
                    Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false);
                } catch (Exception e) {
                    Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, true);
                    // Reassure user
                }
                System.err.println("====== SUCCESS! ======");
            } else if (purchaseState == PurchaseState.REFUNDED || purchaseState == PurchaseState.EXPIRED) {
                try {
                    Preferences.setBoolean(ActFmPreferenceService.PREF_PREMIUM, false);
                    actFmInvoker.invoke("premium_update_android", "purchaseToken", purchaseToken, "productId", itemId);
                    Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false);
                } catch (Exception e) {
                    Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, true);
                }
                System.err.println("====== REFUNDED OR EXPIRED ======");
            }
        }

        @Override
        public void onRequestPurchaseResponse(RequestPurchase request,
                ResponseCode responseCode) {
            if (Constants.DEBUG) {
                Log.d(TAG, request.mProductId + ": " + responseCode);
            }
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Constants.DEBUG) {
                    Log.i(TAG, "purchase was successfully sent to server");
                }
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                if (Constants.DEBUG) {
                    Log.i(TAG, "user canceled purchase");
                }
            } else {
                if (Constants.DEBUG) {
                    Log.i(TAG, "purchase failed");
                }
            }
        }

        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request,
                ResponseCode responseCode) {
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Constants.DEBUG) {
                    Log.d(TAG, "completed RestoreTransactions request");
                }
                // Update the shared preferences so that we don't perform
                // a RestoreTransactions again.
                Preferences.setBoolean(TRANSACTIONS_INITIALIZED, true);
            } else {
                if (Constants.DEBUG) {
                    Log.d(TAG, "RestoreTransactions error: " + responseCode);
                }
            }
        }
    }
}
