package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class DonationActivity extends InjectingAppCompatActivity implements PurchaseHelperCallback {

    private static final int RC_REQUEST = 10001;

    private boolean itemSelected;

    @Inject DialogBuilder dialogBuilder;
    @Inject PurchaseHelper purchaseHelper;

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
                        //noinspection ResultOfMethodCallIgnored
                        matcher.matches();
                        String sku = String.format(Locale.ENGLISH, "%03d", Integer.parseInt(matcher.group(1)));
                        purchaseHelper.purchase(dialogBuilder, DonationActivity.this, sku, null, RC_REQUEST, DonationActivity.this);
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

    private String[] getValues() {
        List<String> values = new ArrayList<>();
        for (int i = 1 ; i <= 100 ; i++) {
            values.add(String.format("$%s USD", Integer.toString(i)));
        }
        return values.toArray(new String[values.size()]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_REQUEST) {
            purchaseHelper.handleActivityResult(this, requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!isChangingConfigurations()) {
            purchaseHelper.disposeIabHelper();
        }
    }

    @Override
    public void purchaseCompleted(boolean success, String sku) {
        finish();
    }
}
