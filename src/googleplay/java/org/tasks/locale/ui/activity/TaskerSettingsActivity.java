package org.tasks.locale.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.bundle.PluginBundleValues;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;

import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public final class TaskerSettingsActivity extends AbstractFragmentPluginAppCompatActivity implements PurchaseHelperCallback, Toolbar.OnMenuItemClickListener {

    private static final int REQUEST_SELECT_FILTER = 10124;
    private static final int REQUEST_PURCHASE = 10125;
    private static final String EXTRA_FILTER = "extra_filter";
    private static final String EXTRA_PURCHASE_INITIATED = "extra_purchase_initiated";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.text_view) TextView filterTitle;

    @Inject Preferences preferences;
    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject PurchaseHelper purchaseHelper;
    @Inject DialogBuilder dialogBuilder;

    private Bundle previousBundle;
    private Filter filter;
    private boolean purchaseInitiated;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasker_settings);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            previousBundle = savedInstanceState.getParcelable(PluginBundleValues.BUNDLE_EXTRA_PREVIOUS_BUNDLE);
            filter = savedInstanceState.getParcelable(EXTRA_FILTER);
            purchaseInitiated = savedInstanceState.getBoolean(EXTRA_PURCHASE_INITIATED);
        } else {
            filter = defaultFilterProvider.getDefaultFilter();
        }
        updateView();

        toolbar.setNavigationIcon(DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_close_24dp)));
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (equalBundles(getResultBundle(), previousBundle)) {
                    cancel();
                } else {
                    dialogBuilder.newMessageDialog(R.string.discard_changes)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancel();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();
                }
            }
        });
        toolbar.inflateMenu(R.menu.tasker_menu);
        MenuColorizer.colorToolbar(this, toolbar);

        if (!preferences.hasPurchase(R.string.p_purchased_tasker) && !purchaseInitiated) {
            purchaseInitiated = purchaseHelper.purchase(dialogBuilder, this, getString(R.string.sku_tasker), getString(R.string.p_purchased_tasker), REQUEST_PURCHASE, this);
        }
    }

    @OnClick(R.id.filter_selection)
    void selectFilter() {
        startActivityForResult(new Intent(TaskerSettingsActivity.this, FilterSelectionActivity.class) {{
            putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
        }}, REQUEST_SELECT_FILTER);
    }

    @Override
    public void onPostCreateWithPreviousResult(final Bundle previousBundle, final String previousBlurb) {
        this.previousBundle = previousBundle;
        this.filter = defaultFilterProvider.getFilterFromPreference(PluginBundleValues.getFilter(previousBundle));
        updateView();
    }

    @Override
    public boolean isBundleValid(final Bundle bundle) {
        return PluginBundleValues.isBundleValid(bundle);
    }

    @Override
    public Bundle getResultBundle() {
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

    private boolean equalBundles(Bundle one, Bundle two) {
        if (one == null) {
            return two == null;
        }
        if (two == null) {
            return false;
        }

        if(one.size() != two.size())
            return false;

        Set<String> setOne = one.keySet();
        Object valueOne;
        Object valueTwo;

        for(String key : setOne) {
            valueOne = one.get(key);
            valueTwo = two.get(key);
            if(valueOne instanceof Bundle && valueTwo instanceof Bundle &&
                    !equalBundles((Bundle) valueOne, (Bundle) valueTwo)) {
                return false;
            }
            else if(valueOne == null) {
                if(valueTwo != null || !two.containsKey(key))
                    return false;
            }
            else if(!valueOne.equals(valueTwo))
                return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_FILTER) {
            if (resultCode == RESULT_OK) {
                filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
                updateView();
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

    private void updateView() {
        filterTitle.setText(filter.listingTitle);
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
