package com.todoroo.astrid.billing;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.timsu.astrid.R;

public class BillingActivity extends Activity {

    private BillingService billingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.billing_activity);

        setupButtons();

        billingService = new BillingService();
        billingService.setContext(this);

        if (!billingService.checkBillingSupported(BillingConstants.ITEM_TYPE_SUBSCRIPTION)) {
            showSubscriptionsNotSupported();
        }
    }

    private void setupButtons() {
        Button buyMonth = (Button) findViewById(R.id.buy_month);
        Button buyYear = (Button) findViewById(R.id.buy_year);

        //TODO: Figure out if we need a payload for any reason

        buyMonth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!billingService.requestPurchase(BillingConstants.PRODUCT_ID_MONTHLY,
                        BillingConstants.ITEM_TYPE_SUBSCRIPTION, null)) {
                    showSubscriptionsNotSupported();
                }
            }
        });

        buyYear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!billingService.requestPurchase(BillingConstants.PRODUCT_ID_YEARLY,
                        BillingConstants.ITEM_TYPE_SUBSCRIPTION, null)) {
                    showSubscriptionsNotSupported();
                }
            }
        });
    }

    private void showSubscriptionsNotSupported() {
        String helpUrl = replaceLanguageAndRegion(getString(R.string.subscriptions_help_url));
        final Uri helpUri = Uri.parse(helpUrl);

        new AlertDialog.Builder(this)
            .setTitle(R.string.subscriptions_not_supported)
            .setMessage(R.string.subscriptions_not_supported_message)
            .setCancelable(false)
            .setPositiveButton(R.string.DLG_ok, null)
            .setNegativeButton(R.string.subscriptions_learn_more, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, helpUri);
                    startActivity(intent);
                }
            }).create().show();
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
}
