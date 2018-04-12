package org.tasks.locale.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.billing.BillingClient;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.bundle.ListNotificationBundle;
import org.tasks.preferences.DefaultFilterProvider;

public final class TaskerSettingsActivity extends AbstractFragmentPluginPreferenceActivity
    implements Toolbar.OnMenuItemClickListener {

  private static final int REQUEST_SELECT_FILTER = 10124;
  private static final int REQUEST_SUBSCRIPTION = 10125;
  private static final String EXTRA_FILTER = "extra_filter";

  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject BillingClient billingClient;
  @Inject Inventory inventory;

  private Bundle previousBundle;
  private Filter filter;

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_tasker);

    if (savedInstanceState != null) {
      previousBundle =
          savedInstanceState.getParcelable(ListNotificationBundle.BUNDLE_EXTRA_PREVIOUS_BUNDLE);
      filter = savedInstanceState.getParcelable(EXTRA_FILTER);
    } else {
      filter = defaultFilterProvider.getDefaultFilter();
    }

    findPreference(R.string.filter)
        .setOnPreferenceClickListener(
            preference -> {
              Intent intent =
                  new Intent(TaskerSettingsActivity.this, FilterSelectionActivity.class);
              intent.putExtra(FilterSelectionActivity.EXTRA_FILTER, filter);
              intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
              startActivityForResult(intent, REQUEST_SELECT_FILTER);
              return false;
            });

    refreshPreferences();

    if (!inventory.purchasedTasker()) {
      startActivityForResult(new Intent(this, PurchaseActivity.class), REQUEST_SUBSCRIPTION);
    }
  }

  @Override
  public void onPostCreateWithPreviousResult(
      final Bundle previousBundle, final String previousBlurb) {
    this.previousBundle = previousBundle;
    this.filter =
        defaultFilterProvider.getFilterFromPreference(
            ListNotificationBundle.getFilter(previousBundle));
    refreshPreferences();
  }

  @Override
  public boolean isBundleValid(final Bundle bundle) {
    return ListNotificationBundle.isBundleValid(bundle);
  }

  @Override
  protected Bundle getResultBundle() {
    return ListNotificationBundle.generateBundle(
        defaultFilterProvider.getFilterPreferenceValue(filter));
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
    } else if (requestCode == REQUEST_SUBSCRIPTION) {
      if (!inventory.purchasedTasker()) {
        cancel();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(ListNotificationBundle.BUNDLE_EXTRA_PREVIOUS_BUNDLE, previousBundle);
    outState.putParcelable(EXTRA_FILTER, filter);
  }

  private void refreshPreferences() {
    findPreference(getString(R.string.filter)).setSummary(filter.listingTitle);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected String getHelpUrl() {
    return "http://tasks.org/help/tasker";
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_save:
        finish();
        return true;
    }
    return super.onMenuItemClick(item);
  }
}
