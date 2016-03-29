package org.tasks.preferences;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.TwoStatePreference;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.receivers.TeslaUnreadReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

public class BasicPreferences extends BaseBasicPreferences implements PurchaseHelperCallback {

    private static final int REQUEST_PURCHASE = 10005;

    @Inject TeslaUnreadReceiver teslaUnreadReceiver;
    @Inject Preferences preferences;
    @Inject PurchaseHelper purchaseHelper;
    @Inject DialogBuilder dialogBuilder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPref(R.string.TLA_menu_donate).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] donationValues = getDonationValues();
                dialogBuilder.newDialog()
                        .setTitle(R.string.select_amount)
                        .setItems(donationValues, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String value = donationValues[which];
                                Pattern pattern = Pattern.compile("\\$(\\d+) USD");
                                Matcher matcher = pattern.matcher(value);
                                //noinspection ResultOfMethodCallIgnored
                                matcher.matches();
                                String sku = String.format(Locale.ENGLISH, "%03d", Integer.parseInt(matcher.group(1)));
                                purchaseHelper.purchase(dialogBuilder, BasicPreferences.this, sku, null, REQUEST_PURCHASE, BasicPreferences.this);
                            }
                        })
                        .show();
                return false;
            }
        });

        getPref(R.string.p_purchased_themes).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && (boolean) newValue && !preferences.hasPurchase(R.string.p_purchased_themes)) {
                    initiateThemePurchase();
                }
                return false;
            }
        });

        getPref(R.string.p_tesla_unread_enabled).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    if ((boolean) newValue && !preferences.hasPurchase(R.string.p_purchased_tesla_unread)) {
                        purchaseHelper.purchase(dialogBuilder, BasicPreferences.this, getString(R.string.sku_tesla_unread), getString(R.string.p_purchased_tesla_unread), REQUEST_PURCHASE, BasicPreferences.this);
                    } else {
                        teslaUnreadReceiver.setEnabled((boolean) newValue);
                        return true;
                    }
                }
                return false;
            }
        });

        getPref(R.string.p_purchased_tasker).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && (boolean) newValue && !preferences.hasPurchase(R.string.p_purchased_tasker)) {
                    purchaseHelper.purchase(dialogBuilder, BasicPreferences.this, getString(R.string.sku_tasker), getString(R.string.p_purchased_tasker), REQUEST_PURCHASE, BasicPreferences.this);
                }
                return false;
            }
        });

        getPref(R.string.p_purchased_dashclock).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null && (boolean) newValue && !preferences.hasPurchase(R.string.p_purchased_dashclock)) {
                    purchaseHelper.purchase(dialogBuilder, BasicPreferences.this, getString(R.string.sku_dashclock), getString(R.string.p_purchased_dashclock), REQUEST_PURCHASE, BasicPreferences.this);
                }
                return false;
            }
        });

        if (BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.preferences_debug);

            findPreference(getString(R.string.debug_unlock_purchases)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    preferences.setBoolean(R.string.p_purchased_dashclock, true);
                    preferences.setBoolean(R.string.p_purchased_tasker, true);
                    preferences.setBoolean(R.string.p_purchased_tesla_unread, true);
                    preferences.setBoolean(R.string.p_purchased_themes, true);
                    recreate();
                    return true;
                }
            });

            findPreference(getString(R.string.debug_consume_purchases)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    purchaseHelper.consumePurchases();
                    recreate();
                    return true;
                }
            });
        }

        requires(R.string.get_plugins, atLeastJellybeanMR1(), R.string.p_purchased_dashclock);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE) {
            purchaseHelper.handleActivityResult(this, requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void purchaseCompleted(final boolean success, final String sku) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getString(R.string.sku_tasker).equals(sku)) {
                    ((TwoStatePreference) getPref(R.string.p_purchased_tasker)).setChecked(success);
                } else if (getString(R.string.sku_tesla_unread).equals(sku)) {
                    ((TwoStatePreference) getPref(R.string.p_tesla_unread_enabled)).setChecked(success);
                } else if (getString(R.string.sku_dashclock).equals(sku)) {
                    ((TwoStatePreference) getPref(R.string.p_purchased_dashclock)).setChecked(success);
                } else if (getString(R.string.sku_themes).equals(sku)) {
                    ((TwoStatePreference) getPref(R.string.p_purchased_themes)).setChecked(success);
                } else {
                    Timber.d("Unhandled sku: %s", sku);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!isChangingConfigurations()) {
            purchaseHelper.disposeIabHelper();
        }
    }

    private String[] getDonationValues() {
        List<String> values = new ArrayList<>();
        for (int i = 1 ; i <= 100 ; i++) {
            values.add(String.format("$%s USD", Integer.toString(i)));
        }
        return values.toArray(new String[values.size()]);
    }

    @Override
    public void initiateThemePurchase() {
        purchaseHelper.purchase(dialogBuilder, this, getString(R.string.sku_themes), getString(R.string.p_purchased_themes), REQUEST_PURCHASE, this);
    }
}
