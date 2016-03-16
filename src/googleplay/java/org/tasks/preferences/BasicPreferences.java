package org.tasks.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.billing.IabHelper;
import org.tasks.billing.IabResult;
import org.tasks.billing.Purchase;
import org.tasks.injection.ActivityComponent;
import org.tasks.receivers.TeslaUnreadReceiver;

import javax.inject.Inject;

public class BasicPreferences extends BaseBasicPreferences implements IabHelper.OnIabPurchaseFinishedListener {

    private static final int REQUEST_PURCHASE_TESLA_UNREAD = 10002;
    private static final int REQUEST_PURCHASE_TASKER = 10003;

    @Inject Tracker tracker;
    @Inject IabHelper iabHelper;
    @Inject TeslaUnreadReceiver teslaUnreadReceiver;
    @Inject Preferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(getString(R.string.p_tesla_unread_enabled)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    if ((boolean) newValue && !preferences.getBoolean(R.string.p_purchased_tesla_unread, BuildConfig.DEBUG)) {
                        iabHelper.launchPurchaseFlow(BasicPreferences.this, getString(R.string.sku_tesla_unread), REQUEST_PURCHASE_TESLA_UNREAD, BasicPreferences.this);
                    } else {
                        teslaUnreadReceiver.setEnabled((boolean) newValue);
                        return true;
                    }
                }
                return false;
            }
        });

        findPreference(getString(R.string.p_tasker_enabled)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    if ((boolean) newValue && !preferences.getBoolean(R.string.p_purchased_tasker, BuildConfig.DEBUG)) {
                        iabHelper.launchPurchaseFlow(BasicPreferences.this, getString(R.string.sku_tasker), REQUEST_PURCHASE_TASKER, BasicPreferences.this);
                    } else {
                        return true;
                    }
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
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, final Purchase info) {
        if (result.isSuccess()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (info.getSku().equals(getString(R.string.sku_tasker))) {
                        preferences.setBoolean(R.string.p_purchased_tasker, true);
                        findPreference(getString(R.string.p_tasker_enabled)).setEnabled(true);
                    } else if (info.getSku().equals(getString(R.string.sku_tesla_unread))) {
                        preferences.setBoolean(R.string.p_purchased_tesla_unread, true);
                        findPreference(getString(R.string.p_tesla_unread_enabled)).setEnabled(true);
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE_TASKER || requestCode == REQUEST_PURCHASE_TESLA_UNREAD) {
            iabHelper.handleActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }
}
