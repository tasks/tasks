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

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.utility.Constants;

public class BillingActivity extends Activity {

    private static final int DIALOG_CANNOT_CONNECT_ID = 1;
    private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;
    private static final int DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID = 3;

    private Handler handler;
    private BillingService billingService;
    private AstridPurchaseObserver purchaseObserver;
    private Button buyMonth;
    private Button buyYear;

    @Autowired private ActFmPreferenceService actFmPreferenceService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.billing_activity);

        setupButtons();

        handler = new Handler();
        billingService = new BillingService();
        billingService.setContext(this);
        purchaseObserver = new AstridPurchaseObserver(this, handler) {
            @Override
            protected void billingSupportedCallback() {
                restoreTransactions();
                buyMonth.setEnabled(true);
                buyYear.setEnabled(true);
            }

            @Override
            protected void billingNotSupportedCallback() {
                showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
            }

            @Override
            protected void subscriptionsNotSupportedCallback() {
                showDialog(DIALOG_SUBSCRIPTIONS_NOT_SUPPORTED_ID);
            }
        };

        ResponseHandler.register(purchaseObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!actFmPreferenceService.isLoggedIn()) {
            // Prompt to log in
            DialogUtilities.okCancelDialog(this, getString(R.string.premium_login_prompt), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent login = new Intent(BillingActivity.this, ActFmLoginActivity.class);
                    startActivity(login);
                }
            },
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        } else if (!billingService.checkBillingSupported(BillingConstants.ITEM_TYPE_SUBSCRIPTION)) {
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
        boolean initialized = Preferences.getBoolean(BillingConstants.PREF_TRANSACTIONS_INITIALIZED, false);
        if (!initialized) {
            billingService.restoreTransactions();
        }
    }
}
