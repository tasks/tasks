package org.tasks.dashclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.api.Filter;

import org.tasks.Broadcaster;
import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class DashClockSettings extends InjectingPreferenceActivity implements PurchaseHelperCallback {

    private static final String EXTRA_PURCHASE_INITIATED = "extra_purchase_initiated";
    private static final int REQUEST_SELECT_FILTER = 1005;
    private static final int REQUEST_PURCHASE = 1006;

    @Inject Preferences preferences;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject Broadcaster broadcaster;
    @Inject PurchaseHelper purchaseHelper;
    @Inject DialogBuilder dialogBuilder;

    private boolean purchaseInitiated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            purchaseInitiated = savedInstanceState.getBoolean(EXTRA_PURCHASE_INITIATED);
        }

        addPreferencesFromResource(R.xml.preferences_dashclock);

        findPreference(getString(R.string.p_dashclock_filter)).setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(DashClockSettings.this, FilterSelectionActivity.class);
            intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
            startActivityForResult(intent, REQUEST_SELECT_FILTER);
            return false;
        });

        refreshPreferences();

        if (!preferences.hasPurchase(R.string.p_purchased_dashclock) && !purchaseInitiated) {
            purchaseHelper.purchase(dialogBuilder, this, getString(R.string.sku_dashclock), getString(R.string.p_purchased_dashclock), REQUEST_PURCHASE, this);
            purchaseInitiated = true;
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
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
                String filterPreference = defaultFilterProvider.getFilterPreferenceValue(filter);
                preferences.setString(R.string.p_dashclock_filter, filterPreference);
                refreshPreferences();
                broadcaster.refresh();
            }
        } else if (requestCode == REQUEST_PURCHASE) {
            purchaseHelper.handleActivityResult(this, requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXTRA_PURCHASE_INITIATED, purchaseInitiated);
    }

    @Override
    public void purchaseCompleted(boolean success, String sku) {
        if (success) {
            broadcaster.refresh();
        } else {
            finish();
        }
    }

    private void refreshPreferences() {
        Filter filter = defaultFilterProvider.getFilterFromPreference(R.string.p_dashclock_filter);
        findPreference(getString(R.string.p_dashclock_filter)).setSummary(filter.listingTitle);
    }
}
