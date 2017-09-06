package org.tasks.locale.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.bundle.PluginBundleValues;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public final class TaskerSettingsActivity extends AbstractFragmentPluginAppCompatActivity implements PurchaseHelperCallback, Toolbar.OnMenuItemClickListener {

    private static final int REQUEST_SELECT_FILTER = 10124;
    private static final int REQUEST_PURCHASE = 10125;
    private static final String EXTRA_FILTER = "extra_filter";
    private static final String EXTRA_PURCHASE_INITIATED = "extra_purchase_initiated";

    @Inject Preferences preferences;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject PurchaseHelper purchaseHelper;

    private Bundle previousBundle;
    private Filter filter;
    private boolean purchaseInitiated;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_tasker);

        if (savedInstanceState != null) {
            previousBundle = savedInstanceState.getParcelable(PluginBundleValues.BUNDLE_EXTRA_PREVIOUS_BUNDLE);
            filter = savedInstanceState.getParcelable(EXTRA_FILTER);
            purchaseInitiated = savedInstanceState.getBoolean(EXTRA_PURCHASE_INITIATED);
        } else {
            filter = defaultFilterProvider.getDefaultFilter();
        }

        findPreference(R.string.filter).setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(TaskerSettingsActivity.this, FilterSelectionActivity.class);
            intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
            startActivityForResult(intent, REQUEST_SELECT_FILTER);
            return false;
        });

        refreshPreferences();

        if (!preferences.hasPurchase(R.string.p_purchased_tasker) && !purchaseInitiated) {
            purchaseInitiated = purchaseHelper.purchase(this, getString(R.string.sku_tasker), getString(R.string.p_purchased_tasker), REQUEST_PURCHASE, this);
        }
    }

    @Override
    public void onPostCreateWithPreviousResult(final Bundle previousBundle, final String previousBlurb) {
        this.previousBundle = previousBundle;
        this.filter = defaultFilterProvider.getFilterFromPreference(PluginBundleValues.getFilter(previousBundle));
        refreshPreferences();
    }

    @Override
    public boolean isBundleValid(final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    protected Bundle getResultBundle() {
        return PluginBundleValues.generateBundle(defaultFilterProvider.getFilterPreferenceValue(filter));
    }

    @Override
    public String getResultBlurb(final Bundle bundle) {
        return filter.listingTitle;
    }

    private void cancel() {
        mIsCancelled = true;
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_FILTER) {
            if (resultCode == RESULT_OK) {
                filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
                refreshPreferences();
            }
        } else if (requestCode == REQUEST_PURCHASE) {
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PluginBundleValues.BUNDLE_EXTRA_PREVIOUS_BUNDLE, previousBundle);
        outState.putParcelable(EXTRA_FILTER, filter);
        outState.putBoolean(EXTRA_PURCHASE_INITIATED, purchaseInitiated);
    }

    private void refreshPreferences() {
        findPreference(getString(R.string.filter)).setSummary(filter.listingTitle);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void purchaseCompleted(boolean success, String sku) {
        if (!success) {
            cancel();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
