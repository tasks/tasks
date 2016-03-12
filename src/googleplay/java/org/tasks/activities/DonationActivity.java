package org.tasks.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.tasks.R;
import org.tasks.billing.IabHelper;
import org.tasks.billing.IabResult;
import org.tasks.billing.Purchase;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import timber.log.Timber;

public class DonationActivity extends InjectingAppCompatActivity implements IabHelper.OnIabPurchaseFinishedListener {

    private static final int RC_REQUEST = 10001;

    private boolean itemSelected;

    @Inject DialogBuilder dialogBuilder;
    @Inject IabHelper iabHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private void initiateDonation(int amount) {
        launchPurchaseFlow(String.format("%03d", amount));
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

    private void error(String message) {
        Timber.e(message);
        Toast.makeText(DonationActivity.this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
        if (result.isSuccess()) {
            Timber.d("Purchased %s", purchase);
        } else {
            error(result.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_REQUEST) {
            iabHelper.handleActivityResult(requestCode, resultCode, data);
            if (resultCode == Activity.RESULT_OK) {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
