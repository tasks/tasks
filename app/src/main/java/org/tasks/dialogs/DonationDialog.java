package org.tasks.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.billing.PurchaseHelper;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.preferences.BasicPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class DonationDialog extends InjectingNativeDialogFragment {

    public static DonationDialog newDonationDialog() {
        return new DonationDialog();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject PurchaseHelper purchaseHelper;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<String> donationValues = getDonationValues();
        return dialogBuilder.newDialog()
                .setTitle(R.string.select_amount)
                .setItems(donationValues, (dialog, which) -> {
                    String value = donationValues.get(which);
                    Pattern pattern = Pattern.compile("\\$(\\d+) USD");
                    Matcher matcher = pattern.matcher(value);
                    //noinspection ResultOfMethodCallIgnored
                    matcher.matches();
                    String sku = String.format(java.util.Locale.ENGLISH, "%03d", Integer.parseInt(matcher.group(1)));
                    purchaseHelper.purchase(getActivity(), sku, null, BasicPreferences.REQUEST_PURCHASE, (BasicPreferences) getActivity());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private List<String> getDonationValues() {
        List<String> values = new ArrayList<>();
        for (int i = 1 ; i <= 100 ; i++) {
            values.add(String.format("$%s USD", Integer.toString(i)));
        }
        return values;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
