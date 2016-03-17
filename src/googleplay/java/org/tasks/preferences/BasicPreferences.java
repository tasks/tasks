package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.receivers.TeslaUnreadReceiver;

import javax.inject.Inject;

import timber.log.Timber;

public class BasicPreferences extends BaseBasicPreferences implements PurchaseHelperCallback {

    private static final int REQUEST_PURCHASE = 10005;

    @Inject Tracker tracker;
    @Inject TeslaUnreadReceiver teslaUnreadReceiver;
    @Inject Preferences preferences;
    @Inject PurchaseHelper purchaseHelper;
    @Inject DialogBuilder dialogBuilder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        findPreference(getString(R.string.p_collect_statistics)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    tracker.setTrackingEnabled((boolean) newValue);
                    return true;
                }
                return false;
            }
        });

        if (BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.preferences_debug);

            findPreference(getString(R.string.debug_consume_purchases)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    purchaseHelper.consumePurchases();
                    return true;
                }
            });
        }
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
    public void purchaseCompleted(boolean success, String sku) {
        if (success) {
            if (getString(R.string.sku_tasker).equals(sku)) {
                getPref(R.string.p_purchased_tasker).setChecked(true);
            } else if (getString(R.string.sku_tesla_unread).equals(sku)) {
                getPref(R.string.p_tesla_unread_enabled).setChecked(true);
            } else if (getString(R.string.sku_dashclock).equals(sku)) {
                getPref(R.string.p_purchased_dashclock).setChecked(true);
            } else {
                Timber.e("Unhandled sku: %s", sku);
            }
        }
    }

    private SwitchPreference getPref(int resId) {
        return (SwitchPreference) findPreference(getString(resId));
    }
}
