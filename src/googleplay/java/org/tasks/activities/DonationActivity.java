package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.billing.IabHelper;
import org.tasks.billing.IabResult;
import org.tasks.billing.Inventory;
import org.tasks.billing.Purchase;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import timber.log.Timber;

public class DonationActivity extends InjectingAppCompatActivity implements IabHelper.OnIabSetupFinishedListener,
        IabHelper.QueryInventoryFinishedListener, IabHelper.OnIabPurchaseFinishedListener,
        IabHelper.OnConsumeFinishedListener, IabHelper.OnConsumeMultiFinishedListener {

    private static final int RC_REQUEST = 10001;

    private IabHelper iabHelper;
    private Inventory inventory;
    private boolean itemSelected;

    @Inject DialogBuilder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        iabHelper = new IabHelper(this, getString(R.string.gp_key));
        if (BuildConfig.DEBUG) {
            iabHelper.enableDebugLogging(true, BuildConfig.APPLICATION_ID);
        }
        iabHelper.startSetup(this);

        final String[] donationValues = getValues();
        dialogBuilder.newDialog()
                .setTitle(R.string.select_amount)
                .setItems(donationValues, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        itemSelected = true;
                        String value = donationValues[which];
                        Pattern pattern = Pattern.compile("\\$(\\d+) USD");
                        Matcher matcher = pattern.matcher(value);
                        if (matcher.matches()) {
                            initiateDonation(Integer.parseInt(matcher.group(1)));
                        } else {
                            error(getString(R.string.error));
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!itemSelected) {
                            finish();
                        }
                    }
                })
                .show();
    }

    private void initiateDonation(int amount) {
        final String sku = String.format("%03d", amount);
        if (inventory != null && inventory.hasPurchase(sku)) {
            iabHelper.consumeAsync(inventory.getPurchase(sku), new IabHelper.OnConsumeFinishedListener() {
                @Override
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                    DonationActivity.this.onConsumeFinished(purchase, result);
                    launchPurchaseFlow(sku);
                }
            });
        } else {
            launchPurchaseFlow(sku);
        }
    }

    private void launchPurchaseFlow(String sku) {
        iabHelper.launchPurchaseFlow(this, sku, RC_REQUEST, this);
    }

    private String[] getValues() {
        List<String> values = new ArrayList<>();
        for (int i = 1 ; i <= 100 ; i++) {
            values.add(String.format("$%s USD", Integer.toString(i)));
        }
        return values.toArray(new String[values.size()]);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (iabHelper != null) {
            iabHelper.dispose();
        }
        iabHelper = null;
    }

    @Override
    public void onIabSetupFinished(IabResult result) {
        if (iabHelper == null) {
            return;
        }

        if (result.isSuccess()) {
            Timber.d("IAB setup successful");
            iabHelper.queryInventoryAsync(this);
        } else {
            error(result.getMessage());
        }
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
        if (iabHelper == null) {
            return;
        }
        if (result.isFailure()) {
            Timber.e("Query inventory failed: %s", result);
        } else {
            this.inventory = inventory;
            iabHelper.consumeAsync(inventory.getAllPurchases(), this);
        }
    }

    private void error(String message) {
        Timber.e(message);
        Toast.makeText(DonationActivity.this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
        if (iabHelper == null) {
            return;
        }

        if (result.isSuccess()) {
            Timber.d("Purchased %s", purchase);
            iabHelper.consumeAsync(purchase, this);
        } else {
            error(result.getMessage());
        }
    }

    @Override
    public void onConsumeFinished(Purchase purchase, IabResult result) {
        if (result.isSuccess()) {
            Timber.d("Consumed %s", purchase);
        } else {
            Timber.e("Error consuming %s: %s", purchase, result);
        }
        finish();
    }

    @Override
    public void onConsumeMultiFinished(List<Purchase> purchases, List<IabResult> results) {
        for (int i = 0 ; i < purchases.size() && i < results.size() ; i++) {
            Timber.d("Consume %s: %s", purchases.get(i), results.get(i));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_REQUEST) {
            String resultString = resultCode == RESULT_OK
                    ? "RESULT_OK"
                    : resultCode == RESULT_CANCELED
                    ? "RESULT_CANCELED"
                    : Integer.toString(resultCode);
            Timber.d("onActivityResult(RC_REQUEST, %s, %s)", resultString, data);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
